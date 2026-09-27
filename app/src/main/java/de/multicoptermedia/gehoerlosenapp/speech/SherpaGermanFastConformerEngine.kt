package de.multicoptermedia.gehoerlosenapp.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
        private const val SAMPLE_RATE = 16_000
        private const val MAX_UTTERANCE_SECONDS = 4
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private var recognizer: OfflineRecognizer? = null
    private var pending = FloatArray(0)
    private var onPartial: (String) -> Unit = {}
    private var onFinal: (String) -> Unit = {}
    private var onStatus: (String) -> Unit = {}
    private var onError: (String) -> Unit = {}

    override val isReady: Boolean
        get() = listOf(MODEL, TOKENS).all(::assetExists)

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit
    ) {
        if (!started.compareAndSet(false, true)) return
        this.onPartial = onPartial
        this.onFinal = onFinal
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
                pending = FloatArray(0)
                if (started.get()) onReady() else releaseRecognizer()
            } catch (_: Throwable) {
                started.set(false)
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

            val oldSize = pending.size
            pending = pending.copyOf(oldSize + samples.size)
            samples.copyInto(pending, oldSize)

            if (pending.size >= SAMPLE_RATE * MAX_UTTERANCE_SECONDS) {
                decodePending()
            }
        }
    }

    override fun stop() {
        started.set(false)
        executor.execute {
            decodePending()
            releaseRecognizer()
        }
    }

    override fun release() {
        started.set(false)
        executor.execute {
            decodePending()
            releaseRecognizer()
        }
        executor.shutdown()
    }

    private fun decodePending() {
        val r = recognizer ?: return
        if (pending.isEmpty()) return

        val audio = pending
        pending = FloatArray(0)
        runCatching {
            val stream = r.createStream()
            stream.acceptWaveform(audio, SAMPLE_RATE)
            r.decode(stream)
            val text = r.getResult(stream).text.trim()
            stream.release()
            if (text.isNotEmpty()) onFinal(text)
            onPartial("")
        }.onFailure {
            onError("Fehler bei der deutschen Offline-Erkennung")
        }
    }

    private fun releaseRecognizer() {
        recognizer?.release()
        recognizer = null
        pending = FloatArray(0)
    }

    private fun assetExists(path: String): Boolean =
        runCatching { context.assets.open(path).use { } }.isSuccess
}
