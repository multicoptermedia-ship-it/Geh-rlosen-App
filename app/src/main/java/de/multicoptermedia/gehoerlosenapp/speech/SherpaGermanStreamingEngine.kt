package de.multicoptermedia.gehoerlosenapp.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Local streaming German ASR backed by sherpa-onnx.
 *
 * Model files are expected in app assets under MODEL_DIR. They are deliberately
 * kept separate from this source file because the weights are large and their
 * redistribution/license must be handled explicitly.
 */
class SherpaGermanStreamingEngine(
    private val context: Context
) : OfflineSpeechEngine {
    companion object {
        const val MODEL_DIR = "sherpa-onnx-streaming-zipformer-de-kroko-2025-08-06"
        private const val ENCODER = "$MODEL_DIR/encoder.onnx"
        private const val DECODER = "$MODEL_DIR/decoder.onnx"
        private const val JOINER = "$MODEL_DIR/joiner.onnx"
        private const val TOKENS = "$MODEL_DIR/tokens.txt"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private val nextSegmentId = AtomicLong(0)
    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var onPartial: (String) -> Unit = {}
    private var onFinal: (RecognizedUtterance) -> Unit = {}
    private var onSpeaker: (SpeakerAssignment) -> Unit = {}
    private var onStatus: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}
    private var lastText = ""

    override val isReady: Boolean
        get() = listOf(ENCODER, DECODER, JOINER, TOKENS).all(::assetExists)

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
                val model = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = ENCODER,
                        decoder = DECODER,
                        joiner = JOINER
                    ),
                    tokens = TOKENS,
                    numThreads = 2,
                    provider = "cpu",
                    modelType = "zipformer2"
                )
                val config = OnlineRecognizerConfig(
                    featConfig = FeatureConfig(sampleRate = 16_000, featureDim = 80),
                    modelConfig = model,
                    enableEndpoint = true,
                    decodingMethod = "greedy_search"
                )
                recognizer = OnlineRecognizer(context.assets, config)
                stream = recognizer?.createStream()
                lastText = ""
                onReady()
            } catch (t: Throwable) {
                started.set(false)
                onError("Offline-Spracherkennung konnte nicht gestartet werden")
            }
        }
    }

    override fun acceptAudio(samples: FloatArray, sampleRate: Int) {
        if (!started.get() || samples.isEmpty()) return
        executor.execute {
            val r = recognizer ?: return@execute
            val s = stream ?: return@execute
            try {
                s.acceptWaveform(samples, sampleRate)
                while (r.isReady(s)) r.decode(s)

                val text = r.getResult(s).text.trim()
                if (text.isNotEmpty() && text != lastText) {
                    lastText = text
                    onPartial(text)
                }

                if (r.isEndpoint(s)) {
                    if (text.isNotEmpty()) onFinal(RecognizedUtterance(nextSegmentId.incrementAndGet(), text))
                    r.reset(s)
                    lastText = ""
                    onPartial("")
                }
            } catch (t: Throwable) {
                onError("Fehler bei der lokalen Spracherkennung")
            }
        }
    }

    override fun stop() {
        started.set(false)
        executor.execute {
            val r = recognizer
            val s = stream
            if (r != null && s != null) {
                runCatching {
                    s.inputFinished()
                    while (r.isReady(s)) r.decode(s)
                    val text = r.getResult(s).text.trim()
                    if (text.isNotEmpty()) onFinal(RecognizedUtterance(nextSegmentId.incrementAndGet(), text))
                }
            }
            releaseRecognizer()
        }
    }

    override fun release() {
        started.set(false)
        executor.execute { releaseRecognizer() }
        executor.shutdown()
    }

    private fun releaseRecognizer() {
        stream?.release()
        stream = null
        recognizer?.release()
        recognizer = null
        lastText = ""
    }

    private fun assetExists(path: String): Boolean =
        runCatching { context.assets.open(path).use { } }.isSuccess
}
