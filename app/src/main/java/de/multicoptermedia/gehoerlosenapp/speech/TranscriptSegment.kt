package de.multicoptermedia.gehoerlosenapp.speech

/**
 * Text plus optional speaker assignment.
 *
 * speakerId is deliberately neutral: it describes only whether two speech
 * segments belong to the same locally detected speaker. It does not infer
 * gender or identity.
 */
data class TranscriptSegment(
    val segmentId: Long? = null,
    val text: String,
    val speakerId: Int? = null,
    val isFinal: Boolean = true
)

interface SpeakerDiarizationEngine {
    fun identifySpeaker(samples: FloatArray, sampleRate: Int): Int?
    fun reset()
    fun release()
}
