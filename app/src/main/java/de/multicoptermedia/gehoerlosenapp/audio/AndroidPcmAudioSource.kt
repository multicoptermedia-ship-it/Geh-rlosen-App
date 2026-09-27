package de.multicoptermedia.gehoerlosenapp.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

class AndroidPcmAudioSource : PcmAudioSource {
    companion object {
        const val SAMPLE_RATE = 16_000
    }

    private val running = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
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

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuffer * 2
        )
        audioRecord = record

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            running.set(false)
            record.release()
            audioRecord = null
            onError("Mikrofon ist auf diesem Gerät nicht verfügbar")
            return
        }

        record.startRecording()
        worker = Thread {
            val pcm = ShortArray(minBuffer)
            try {
                while (running.get()) {
                    val count = record.read(pcm, 0, pcm.size)
                    if (count > 0) {
                        val samples = FloatArray(count)
                        for (i in 0 until count) samples[i] = pcm[i] / 32768.0f
                        onSamples(samples)
                    } else if (count < 0) {
                        onError("Fehler beim Lesen des Mikrofons: " + count)
                        break
                    }
                }
            } finally {
                running.set(false)
            }
        }.apply {
            name = "conversation-pcm-capture"
            start()
        }
    }

    override fun stop() {
        if (!running.getAndSet(false)) return
        runCatching { audioRecord?.stop() }
        worker?.join(500)
        worker = null
    }

    override fun release() {
        stop()
        audioRecord?.release()
        audioRecord = null
    }
}
