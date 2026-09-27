package de.multicoptermedia.gehoerlosenapp.speech

/**
 * Keeps neutral, session-local speaker assignments.
 *
 * The tracker deliberately knows nothing about names, gender or identity.
 * It only maps comparable voice embeddings to Person 1, Person 2, ...
 *
 * Embeddings and assignments live in memory only and are discarded when the
 * current listening session is reset/released.
 */
class LocalSpeakerTracker(
    private val similarityThreshold: Float = 0.72f
) {
    private val centroids = mutableListOf<FloatArray>()
    private val counts = mutableListOf<Int>()

    @Synchronized
    fun assign(embedding: FloatArray): Int? {
        if (embedding.isEmpty()) return null

        val normalized = normalize(embedding) ?: return null
        if (centroids.isEmpty()) {
            centroids += normalized
            counts += 1
            return 0
        }

        var bestIndex = -1
        var bestSimilarity = -1f
        for (index in centroids.indices) {
            val similarity = cosine(normalized, centroids[index])
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestIndex = index
            }
        }

        if (bestIndex >= 0 && bestSimilarity >= similarityThreshold) {
            updateCentroid(bestIndex, normalized)
            return bestIndex
        }

        centroids += normalized
        counts += 1
        return centroids.lastIndex
    }

    @Synchronized
    fun reset() {
        centroids.clear()
        counts.clear()
    }

    @Synchronized
    fun speakerCount(): Int = centroids.size

    private fun updateCentroid(index: Int, embedding: FloatArray) {
        val old = centroids[index]
        val count = counts[index]
        val updated = FloatArray(old.size)
        for (i in old.indices) {
            updated[i] = (old[i] * count + embedding[i]) / (count + 1)
        }
        centroids[index] = normalize(updated) ?: old
        counts[index] = count + 1
    }

    private fun normalize(values: FloatArray): FloatArray? {
        var squaredSum = 0.0
        for (value in values) squaredSum += value * value
        if (squaredSum <= 0.0) return null

        val norm = kotlin.math.sqrt(squaredSum).toFloat()
        return FloatArray(values.size) { index -> values[index] / norm }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return -1f
        var sum = 0f
        for (i in a.indices) sum += a[i] * b[i]
        return sum
    }
}
