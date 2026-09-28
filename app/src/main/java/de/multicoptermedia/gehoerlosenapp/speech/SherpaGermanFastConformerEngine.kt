package de.multicoptermedia.gehoerlosenapp.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Redistributable German ASR path based on NVIDIA FastConformer CTC.
 *
 * Audio is collected into short utterances and decoded locally. A VAD front-end
 * can feed complete speech segments into this engine without any network access.
 */
class SherpaGermanFastConformerEngine(
    private val context: Context
) : OfflineSpeechEngine {
    companion object {
        const val MODEL_DIR = "sherpa-onnx-nemo-stt_de_fastconformer_hybrid_large_pc-int8"
        private const val MODEL = "$MODEL_DIR/model.int8.onnx"
        private const val TOKENS = "$MODEL_DIR/tokens.txt"
        private const val VAD_MODEL = "silero_vad.onnx"
        private const val SPEAKER_MODEL = "wespeaker_en_voxceleb_resnet34.onnx"
        private const val MIN_SPEAKER_SAMPLES = 16_000
        private const val SAMPLE_RATE = 16_000
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val speakerExecutor = Executors.newSingleThreadExecutor()
    private val speakerTracker = LocalSpeakerTracker()
    private val started = AtomicBoolean(false)
    private val nextSegmentId = AtomicLong(0)
    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    @Volatile private var speakerEngine: SherpaSpeakerEmbeddingEngine? = null
    private var onPartial: (String) -> Unit = {}
    private var onFinal: (RecognizedUtterance) -> Unit = {}
    private var onSpeaker: (SpeakerAssignment) -> Unit = {}
    private var onStatus: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}

    override val isReady: Boolean
        get() = listOf(MODEL, TOKENS, VAD_MODEL).all(::assetExists)

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (RecognizedUtterance) -> Unit,
        onSpeaker: (SpeakerAssignment) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit
    ) {
        if (!started.compareAndSet(false, true)) return
        this.onPartial = onPartial
        this.onFinal = onFinal
        this.onSpeaker = onSpeaker
        this.onStatus = onStatus
        this.onError = onError

        executor.execute {
            try {
                if (!isReady) {
                    started.set(false)
                    onStatus("Deutsches Offline-Sprachmodell fehlt")
                    return@execute
                }

                val modelConfig = OfflineModelConfig(
                    nemo = OfflineNemoEncDecCtcModelConfig(model = MODEL),
                    tokens = TOKENS,
                    numThreads = 2,
                    provider = "cpu",
                    modelType = "nemo_ctc"
                )
                recognizer = OfflineRecognizer(
                    context.assets,
                    OfflineRecognizerConfig(
                        featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                        modelConfig = modelConfig,
                        decodingMethod = "greedy_search"
                    )
                )
                vad = Vad(
                    context.assets,
                    VadModelConfig(
                        sileroVadModelConfig = SileroVadModelConfig(
                            model = VAD_MODEL,
                            threshold = 0.5F,
                            minSilenceDuration = 0.35F,
                            minSpeechDuration = 0.20F,
                            windowSize = 512,
                            maxSpeechDuration = 12.0F
                        ),
                        sampleRate = SAMPLE_RATE,
                        numThreads = 1,
                        provider = "cpu"
                    )
                )
                runCatching {
                    if (assetExists(SPEAKER_MODEL)) {
                        speakerEngine = SherpaSpeakerEmbeddingEngine(context, SPEAKER_MODEL).also { it.start() }
                        speakerTracker.reset()
                    }
                }
                if (started.get()) onReady() else releaseRecognizer()
            } catch (_: Throwable) {
                started.set(false)
                releaseRecognizer()
                onError("Deutsche Offline-Spracherkennung konnte nicht gestartet werden")
            }
        }
    }

    override fun acceptAudio(samples: FloatArray, sampleRate: Int) {
        if (!started.get() || samples.isEmpty()) return
        executor.execute {
            if (!started.get()) return@execute
            if (sampleRate != SAMPLE_RATE) {
                onError("Unerwartete Mikrofon-Abtastrate")
                return@execute
            }

            try {
                val detector = vad ?: return@execute
                detector.acceptWaveform(samples)
                while (!detector.empty()) {
                    val segment = detector.front()
                    decodeSegment(segment.samples)
                    detector.pop()
                }
            } catch (_: Throwable) {
                started.set(false)
                releaseRecognizer()
                onError("Fehler bei der lokalen Spracherkennung")
            }
        }
    }

    override fun stop() {
        started.set(false)
        executor.execute {
            flushVad()
            releaseRecognizer()
        }
    }

    override fun release() {
        started.set(false)
        executor.execute {
            flushVad()
            releaseRecognizer()
        }
        executor.shutdown()
        speakerExecutor.shutdown()
    }

    private fun decodeSegment(audio: FloatArray) {
        val r = recognizer ?: return
        if (audio.isEmpty()) return

        val stream = r.createStream()
        try {
            stream.acceptWaveform(audio, SAMPLE_RATE)
            r.decode(stream)
            val text = r.getResult(stream).text.trim()
            if (text.isNotEmpty()) {
                val segmentId = nextSegmentId.incrementAndGet()
                onFinal(RecognizedUtterance(segmentId = segmentId, text = text))
                // Text is delivered first. Speaker work runs independently afterwards.
                if (audio.size >= MIN_SPEAKER_SAMPLES) {
                    val speakerAudio = audio.copyOf()
                    speakerExecutor.execute {
                        runCatching {
                            val embedding = speakerEngine?.compute(speakerAudio, SAMPLE_RATE)
                            val speakerId = embedding?.let(speakerTracker::assign)
                            if (speakerId != null && started.get()) {
                                onSpeaker(SpeakerAssignment(segmentId, speakerId))
                            }
                        }
                    }
                }
            }
            onPartial("")
        } finally {
            stream.release()
        }
    }

    private fun flushVad() {
        val detector = vad ?: return
        runCatching {
            detector.flush()
            while (!detector.empty()) {
                val segment = detector.front()
                decodeSegment(segment.samples)
                detector.pop()
            }
        }
    }

    private fun releaseRecognizer() {
        vad?.release()
        vad = null
        speakerEngine?.release()
        speakerEngine = null
        speakerTracker.reset()
        recognizer?.release()
        recognizer = null
    }

    private fun assetExists(path: String): Boolean =
        runCatching { context.assets.open(path).use { } }.isSuccess
}
