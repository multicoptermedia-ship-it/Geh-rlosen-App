package de.multicoptermedia.gehoerlosenapp.audio

interface PcmAudioSource {
    fun start(onSamples: (FloatArray) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun release()
}
