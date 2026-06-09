package com.example.vellum.data

import java.util.Locale

object SemanticMatcher {
    private const val EMBEDDING_DIM = 5
    
    // 5 concepts:
    // index 0: Food/Dining
    // index 1: Transport/Auto
    // index 2: Shopping/Retail
    // index 3: Income/Job/Salary
    // index 4: Housing/Utilities/Bills
    private val vocabulary = mapOf(
        "food" to floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f),
        "dinner" to floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f, 0.0f),
        "lunch" to floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f, 0.0f),
        "restaurant" to floatArrayOf(0.95f, 0.0f, 0.0f, 0.0f, 0.0f),
        "cafe" to floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f, 0.0f),
        "coffee" to floatArrayOf(0.85f, 0.0f, 0.0f, 0.0f, 0.0f),
        "eat" to floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f, 0.0f),
        "eating" to floatArrayOf(0.9f, 0.0f, 0.0f, 0.0f, 0.0f),
        "burger" to floatArrayOf(0.85f, 0.0f, 0.0f, 0.0f, 0.0f),
        "pizza" to floatArrayOf(0.85f, 0.0f, 0.0f, 0.0f, 0.0f),
        "groceries" to floatArrayOf(0.8f, 0.0f, 0.2f, 0.0f, 0.0f),
        "grocery" to floatArrayOf(0.8f, 0.0f, 0.2f, 0.0f, 0.0f),
        "supermarket" to floatArrayOf(0.6f, 0.0f, 0.6f, 0.0f, 0.0f),
        
        "fuel" to floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f),
        "gas" to floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f),
        "petrol" to floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f),
        "diesel" to floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f),
        "car" to floatArrayOf(0.0f, 0.85f, 0.0f, 0.0f, 0.0f),
        "taxi" to floatArrayOf(0.0f, 0.9f, 0.0f, 0.0f, 0.0f),
        "cab" to floatArrayOf(0.0f, 0.9f, 0.0f, 0.0f, 0.0f),
        "travel" to floatArrayOf(0.0f, 0.8f, 0.0f, 0.0f, 0.0f),
        "flight" to floatArrayOf(0.0f, 0.75f, 0.0f, 0.0f, 0.0f),
        "trip" to floatArrayOf(0.0f, 0.8f, 0.0f, 0.0f, 0.0f),
        
        "shopping" to floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f),
        "clothes" to floatArrayOf(0.0f, 0.0f, 0.9f, 0.0f, 0.0f),
        "clothing" to floatArrayOf(0.0f, 0.0f, 0.9f, 0.0f, 0.0f),
        "shirt" to floatArrayOf(0.0f, 0.0f, 0.85f, 0.0f, 0.0f),
        "shoes" to floatArrayOf(0.0f, 0.0f, 0.85f, 0.0f, 0.0f),
        "buy" to floatArrayOf(0.0f, 0.0f, 0.7f, 0.0f, 0.0f),
        "store" to floatArrayOf(0.0f, 0.0f, 0.8f, 0.0f, 0.0f),
        "mall" to floatArrayOf(0.0f, 0.0f, 0.9f, 0.0f, 0.0f),
        
        "salary" to floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f),
        "income" to floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f),
        "pay" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.8f, 0.0f),
        "paycheck" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.95f, 0.0f),
        "bonus" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.9f, 0.0f),
        "dividend" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.8f, 0.0f),
        
        "rent" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 1.0f),
        "electricity" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.9f),
        "water" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.8f),
        "wifi" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.8f),
        "internet" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.8f),
        "bill" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.85f),
        "bills" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.85f),
        "utilities" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.9f),
        "utility" to floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 0.9f),
        "subscription" to floatArrayOf(0.0f, 0.0f, 0.05f, 0.0f, 0.8f)
    )

    private fun getVector(text: String): FloatArray {
        val words = text.lowercase(Locale.US).split(Regex("[\\s_\\-\\.\\,\\(\\)\\{\\}\\[\\]\\!\\?\\*]+"))
        val sum = FloatArray(EMBEDDING_DIM)
        var count = 0
        for (word in words) {
            val vec = vocabulary[word]
            if (vec != null) {
                for (i in 0 until EMBEDDING_DIM) {
                    sum[i] += vec[i]
                }
                count++
            }
        }
        if (count > 0) {
            for (i in 0 until EMBEDDING_DIM) {
                sum[i] /= count
            }
        }
        return sum
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in 0 until EMBEDDING_DIM) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        if (normA == 0.0f || normB == 0.0f) return 0.0f
        return (dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble()))).toFloat()
    }

    fun isSemanticMatch(query: String, targetText: String, threshold: Float = 0.35f): Boolean {
        val qClean = query.lowercase(Locale.US).trim()
        val tClean = targetText.lowercase(Locale.US).trim()
        if (qClean.isEmpty() || tClean.isEmpty()) return false
        
        // Check direct containment matches
        if (tClean.contains(qClean) || qClean.contains(tClean)) {
            return true
        }
        
        val qVec = getVector(query)
        val tVec = getVector(targetText)
        val similarity = cosineSimilarity(qVec, tVec)
        return similarity >= threshold
    }
}
