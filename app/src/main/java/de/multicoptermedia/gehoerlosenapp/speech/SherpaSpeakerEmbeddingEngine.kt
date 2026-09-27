package de.multicoptermedia.gehoerlosenapp.speech

import android.content.Context
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig

/**
 * Local speaker embedding backend for sherpa-onnx.
 *
 * No identity, gender or network lookup is performed. The model only converts
 * one speech segment into a numeric voice embedding that LocalSpeakerTracker
 * can compare within the current session.
 */
class SherpaSpeakerEmbeddingEngine(
    context: Context,
    private val modelAssetName: String
) {
    private val assetManager = context.applicationContext.assets
    private var extractor: SpeakerEmbeddingExtractor? = null

    @Synchronized
    fun start() {
        if (extractor != null) return
        extractor = SpeakerEmbeddingExtractor(
            assetManager = assetManager,
            config = SpeakerEmbeddingExtractorConfig(
                model = modelAssetName,
                numThreads = 1,
                debug = false,
                provider = "cpu"
            )
        )
    }

    @Synchronized
    fun compute(samples: FloatArray, sampleRate: Int): FloatArray? {
        if (sampleRate != 16_000 || samples.isEmpty()) return null
        val activeExtractor = extractor ?: return null
        val stream = activeExtractor.createStream()
        return try {
            stream.acceptWaveform(samples, sampleRate)
            stream.inputFinished()
            if (!activeExtractor.isReady(stream)) null else activeExtractor.compute(stream)
        } finally {
            stream.release()
        }
    }

    @Synchronized
    fun release() {
        extractor?.release()
        extractor = null
    }
}
