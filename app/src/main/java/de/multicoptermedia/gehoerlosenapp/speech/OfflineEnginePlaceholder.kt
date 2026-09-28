package de.multicoptermedia.gehoerlosenapp.speech

/**
 * Temporary adapter point for the local ASR implementation.
 *
 * The next integration step connects sherpa-onnx here. Keeping the UI and
 * microphone capture behind interfaces prevents Android SpeechRecognizer or
 * a specific model from becoming a hard dependency of the conversation UI.
 */
class OfflineEnginePlaceholder : OfflineSpeechEngine {
    override val isReady: Boolean = false

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (RecognizedUtterance) -> Unit,
        onSpeaker: (SpeakerAssignment) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit
    ) {
        onStatus("Offline-Sprachmodell wird vorbereitet")
    }

    override fun acceptAudio(samples: FloatArray, sampleRate: Int) = Unit
    override fun stop() = Unit
    override fun release() = Unit
}
