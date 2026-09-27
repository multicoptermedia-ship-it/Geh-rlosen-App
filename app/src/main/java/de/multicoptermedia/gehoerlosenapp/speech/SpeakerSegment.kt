package de.multicoptermedia.gehoerlosenapp.speech

/**
 * One readable utterance assigned to a locally detected speaker identity.
 *
 * speakerId deliberately represents only "same/different voice". It does not
 * infer gender, age or identity.
 */
data class SpeakerSegment(
    val speakerId: Int,
    val text: String
)
