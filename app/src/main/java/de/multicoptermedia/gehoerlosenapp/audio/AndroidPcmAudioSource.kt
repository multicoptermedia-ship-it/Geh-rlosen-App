package de.multicoptermedia.gehoerlosenapp.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import java.util.concurrent.atomic.AtomicBoolean

enum class AudioProcessingMode {
    NOISE_SUPPRESSION,
    ORIGINAL
}

class AndroidPcmAudioSource(
    private val processingMode: AudioProcessingMode = AudioProcessingMode.NOISE_SUPPRESSION
) : PcmAudioSource {
    companion object {
        const val SAMPLE_RATE = 16_000
    }

    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var worker: Thread? = null

    @SuppressLint("MissingPermission")
    override fun start(onSamples: (FloatArray) -> Unit, onError: (String) -> Unit) {
        if (!running.compareAndSet(false, true)) return

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            running.set(false)
            onError("Mikrofon konnte nicht initialisiert werden")
            return
        }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2
            )
        } catch (_: Throwable) {
            running.set(false)
            onError("Mikrofon konnte nicht geöffnet werden")
            return
        }
        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            running.set(false)
            record.release()
            audioRecord = null
            onError("Mikrofon ist auf diesem Gerät nicht verfügbar")
            return
        }

        noiseSuppressor = if (
            processingMode == AudioProcessingMode.NOISE_SUPPRESSION &&
            NoiseSuppressor.isAvailable()
        ) {
            runCatching {
                NoiseSuppressor.create(record.audioSessionId)?.apply { enabled = true }
            }.getOrNull()
        } else {
            null
        }

        try {
            record.startRecording()
        } catch (_: Throwable) {
            running.set(false)
            noiseSuppressor?.release()
            noiseSuppressor = null
            record.release()
            audioRecord = null
            onError("Mikrofon konnte nicht gestartet werden")
            return
        }

        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            running.set(false)
            noiseSuppressor?.release()
            noiseSuppressor = null
            record.release()
            audioRecord = null
            onError("Mikrofon nimmt nicht auf")
            return
        }

        worker = Thread {
            val pcm = ShortArray((minBuffer / 2).coerceAtLeast(1))
            try {
                while (running.get()) {
                    val count = record.read(pcm, 0, pcm.size)
                    if (count > 0) {
                        val samples = FloatArray(count)
                        for (i in 0 until count) samples[i] = pcm[i] / 32768.0f
                        onSamples(samples)
                    } else if (count < 0) {
                        running.set(false)
                        onError("Fehler beim Lesen des Mikrofons: " + count)
                        break
                    }
                }
            } catch (_: Throwable) {
                running.set(false)
                onError("Mikrofonaufnahme wurde unterbrochen")
            } finally {
                running.set(false)
            }
        }.apply {
            name = "conversation-pcm-capture"
            start()
        }
    }

    override fun stop() {
        val wasRunning = running.getAndSet(false)
        val record = audioRecord
        audioRecord = null
        val suppressor = noiseSuppressor
        noiseSuppressor = null

        if (wasRunning) {
            runCatching { record?.stop() }
        }
        worker?.join(500)
        worker = null
        runCatching { suppressor?.release() }
        runCatching { record?.release() }
    }

    override fun release() {
        stop()
    }
}
