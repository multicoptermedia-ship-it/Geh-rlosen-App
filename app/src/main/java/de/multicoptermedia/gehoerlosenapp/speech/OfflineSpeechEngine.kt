package de.multicoptermedia.gehoerlosenapp.speech

interface OfflineSpeechEngine {
    val isReady: Boolean

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun acceptAudio(samples: FloatArray, sampleRate: Int)
    fun stop()
    fun release()
}
