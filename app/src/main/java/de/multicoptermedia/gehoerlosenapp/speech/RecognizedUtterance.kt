package de.multicoptermedia.gehoerlosenapp.speech

/**
 * One locally recognized speech segment.
 *
 * segmentId lets the UI show text immediately and attach a neutral speaker
 * assignment later without delaying transcription.
 */
data class RecognizedUtterance(
    val segmentId: Long,
    val text: String
)

data class SpeakerAssignment(
    val segmentId: Long,
    val speakerId: Int
)
