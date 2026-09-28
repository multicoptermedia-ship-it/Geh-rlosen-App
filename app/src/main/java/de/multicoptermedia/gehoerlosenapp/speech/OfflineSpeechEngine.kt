package de.multicoptermedia.gehoerlosenapp.speech

interface OfflineSpeechEngine {
    val isReady: Boolean

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (RecognizedUtterance) -> Unit,
        onSpeaker: (SpeakerAssignment) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit,
        onReady: () -> Unit
    )

    fun acceptAudio(samples: FloatArray, sampleRate: Int)
    fun stop()
    fun release()
}
