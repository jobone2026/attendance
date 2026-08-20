package com.example.domain.ml

import kotlin.math.sqrt

class FaceMatcher {

    companion object {
        const val DEFAULT_THRESHOLD = 0.68f
    }

    /**
     * Calculates cosine similarity between two unit vectors.
     * Range: -1.0 to 1.0 (Higher means greater similarity).
     */
    fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
        if (v1.isEmpty() || v2.isEmpty() || v1.size != v2.size) return 0f

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in v1.indices) {
            val a = v1[i]
            val b = v2[i]
            dotProduct += a * b
            normA += a * a
            normB += b * b
        }

        val denominator = (sqrt(normA.toDouble()) * sqrt(normB.toDouble())).toFloat()
        if (denominator < 1e-6f) return 0f

        return (dotProduct / denominator).coerceIn(0f, 1f)
    }

    /**
     * Aggregates 3-5 multi-angle sample embeddings into a robust enrolled master embedding.
     */
    fun aggregateEmbeddings(samples: List<List<Float>>): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val dim = samples.first().size
        val average = FloatArray(dim)

        for (sample in samples) {
            for (i in 0 until dim) {
                if (i < sample.size) {
                    average[i] += sample[i]
                }
            }
        }

        // L2 Normalize average vector
        var sumSq = 0.0f
        for (i in 0 until dim) {
            average[i] /= samples.size
            sumSq += average[i] * average[i]
        }
        val norm = sqrt(sumSq.toDouble()).toFloat().coerceAtLeast(1e-6f)
        for (i in 0 until dim) {
            average[i] /= norm
        }

        return average.toList()
    }
}
