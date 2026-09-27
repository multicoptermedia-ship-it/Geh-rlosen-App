package de.multicoptermedia.gehoerlosenapp.speech

import de.multicoptermedia.gehoerlosenapp.audio.AndroidPcmAudioSource
import de.multicoptermedia.gehoerlosenapp.audio.PcmAudioSource
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

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
        onError: (String) -> Unit,
        onLevel: (Float) -> Unit = {}
    ) {
        if (!running.compareAndSet(false, true)) return
        if (!speechEngine.isReady) {
            running.set(false)
            onStatus("Offline-Sprachmodell noch nicht installiert")
            return
        }

        onStatus("Offline-Spracherkennung wird geladen …")
        speechEngine.start(
            onPartial = onPartial,
            onFinal = onFinal,
            onStatus = onStatus,
            onError = { message ->
                if (running.getAndSet(false)) {
                    audioSource.stop()
                }
                onError(message)
            },
            onReady = {
                if (!running.get()) {
                    speechEngine.stop()
                    return@start
                }
                audioSource.start(
                    onSamples = { samples ->
                        if (running.get()) {
                            var energy = 0.0
                            for (sample in samples) energy += sample * sample
                            val rms = if (samples.isEmpty()) 0f else sqrt(energy / samples.size).toFloat()
                            onLevel(rms.coerceIn(0f, 1f))
                            speechEngine.acceptAudio(samples, AndroidPcmAudioSource.SAMPLE_RATE)
                        }
                    },
                    onError = { message ->
                        running.set(false)
                        speechEngine.stop()
                        onError(message)
                    }
                )
                if (running.get()) onStatus("● Offline · Ich höre zu")
            }
        )
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
