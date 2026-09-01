package com.marunthu.core.medicine

import com.marunthu.core.model.Medicine
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.ocr.TextNormalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Resolves noisy OCR text to ranked [MedicineCandidate]s against the local catalog.
 * Pure Kotlin — unit tested. Tolerant of OCR errors ("METFORNIN" -> Metformin) but
 * NEVER silently certain: every match carries a confidence the UX gates on.
 */
class MedicineMatcher(private val catalog: List<Medicine>) {

    /** @return candidates sorted by descending confidence, best first. */
    fun match(ocrText: String, limit: Int = 5): List<MedicineCandidate> {
        val queryTokens = TextNormalizer.tokens(ocrText)
        if (queryTokens.isEmpty()) return emptyList()
        val strength = TextNormalizer.extractStrength(ocrText)

        return catalog.map { med ->
            val nameScore = bestTokenScore(queryTokens, med.searchableTokens)
            val strengthBonus = strengthAgreement(strength, med)
            // Weighted: name dominates; strength nudges among same-brand variants.
            val confidence = (nameScore * 0.85 + strengthBonus * 0.15).coerceIn(0.0, 1.0)
            MedicineCandidate(med, confidence, ocrText.trim())
        }.filter { it.confidence > 0.30 }
            .sortedByDescending { it.confidence }
            .take(limit)
    }

    /** Best fuzzy similarity between any query token and any catalog token. */
    private fun bestTokenScore(query: List<String>, target: List<String>): Double {
        var best = 0.0
        for (q in query) for (t in target) {
            best = max(best, similarity(q, t))
            if (best == 1.0) return 1.0
        }
        return best
    }

    private fun strengthAgreement(strength: Pair<Double, String>?, med: Medicine): Double {
        if (strength == null || med.strengthValue == null) return 0.5 // neutral
        val sameUnit = strength.second.equals(med.strengthUnit, ignoreCase = true)
        val sameValue = strength.first == med.strengthValue
        return when {
            sameUnit && sameValue -> 1.0
            sameValue -> 0.7
            else -> 0.0
        }
    }

    /** Normalized similarity in [0,1] from Levenshtein edit distance. */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val d = levenshtein(a, b)
        val len = max(a.length, b.length)
        return if (len == 0) 0.0 else 1.0 - d.toDouble() / len
    }

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }
}
