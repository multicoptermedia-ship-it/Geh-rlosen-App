package de.multicoptermedia.gehoerlosenapp.speech

import de.multicoptermedia.gehoerlosenapp.audio.AndroidPcmAudioSource
import de.multicoptermedia.gehoerlosenapp.audio.PcmAudioSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the real-time audio path. The UI talks only to this controller and
 * therefore does not need to know which local ASR or diarization model is used.
 */
class ConversationAudioController(
    private val audioSource: PcmAudioSource = AndroidPcmAudioSource(),
    private val speechEngine: OfflineSpeechEngine
) {
    private val running = AtomicBoolean(false)

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onStatus: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!running.compareAndSet(false, true)) return
        if (!speechEngine.isReady) {
            running.set(false)
            onStatus("Offline-Sprachmodell noch nicht installiert")
            return
        }

        speechEngine.start(onPartial, onFinal, onStatus, onError)
        audioSource.start(
            onSamples = { samples ->
                if (running.get()) {
                    speechEngine.acceptAudio(samples, AndroidPcmAudioSource.SAMPLE_RATE)
                }
            },
            onError = { message ->
                running.set(false)
                speechEngine.stop()
                onError(message)
            }
        )
        onStatus("● Ich höre zu")
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        audioSource.stop()
        speechEngine.stop()
    }

    fun release() {
        running.set(false)
        audioSource.release()
        speechEngine.release()
    }
}
