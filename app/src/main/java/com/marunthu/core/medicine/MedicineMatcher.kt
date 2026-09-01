package com.marunthu.core.medicine

import com.marunthu.core.model.Medicine
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.ocr.TextNormalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Resolves noisy OCR text to ranked [MedicineCandidate]s against the local catalog.
 * Pure Kotlin — unit tested. Tolerant of OCR errors ("METFORNIN" -> Metformin) but never
 * silently certain: every match carries a confidence the UX gates on.
 *
 * Scales to tens of thousands of medicines via a 3-char prefix inverted index: instead of
 * fuzzy-scoring the whole catalog on every scan, we only score medicines that share a token
 * prefix with the OCR text. Built once at construction.
 */
class MedicineMatcher(private val catalog: List<Medicine>) {

    // token-prefix(3 chars) -> medicine indices that contain a token with that prefix
    private val index: HashMap<String, MutableList<Int>> = HashMap()

    init {
        catalog.forEachIndexed { idx, med ->
            med.searchableTokens.forEach { tok ->
                if (tok.length >= 3) index.getOrPut(tok.substring(0, 3)) { ArrayList() }.add(idx)
            }
        }
    }

    /** @return candidates sorted by descending confidence, best first. */
    fun match(ocrText: String, limit: Int = 5): List<MedicineCandidate> {
        val queryTokens = TextNormalizer.tokens(ocrText)
        if (queryTokens.isEmpty()) return emptyList()
        val strength = TextNormalizer.extractStrength(ocrText)

        // Gather candidate medicine indices sharing a 3-char token prefix with the query.
        val candidates = LinkedHashSet<Int>()
        for (q in queryTokens) if (q.length >= 3) index[q.substring(0, 3)]?.let { candidates.addAll(it) }
        // Small catalogs (demo) or a total miss: fall back to scanning everything.
        val pool: Iterable<Int> =
            if (candidates.isEmpty()) {
                if (catalog.size <= 500) catalog.indices else return emptyList()
            } else candidates

        return pool.asSequence()
            .map { catalog[it] }
            .map { med ->
                val nameScore = bestTokenScore(queryTokens, med.searchableTokens)
                val strengthBonus = strengthAgreement(strength, med)
                // Reward medicines where 2+ of their tokens (brand + generic) strongly match
                // the OCR text — favours a full-name hit over a coincidental single-token match.
                val strongMatches = med.searchableTokens.count { t ->
                    t.length >= 4 && queryTokens.any { q -> similarity(q, t) >= 0.8 }
                }
                val multiBonus = if (strongMatches >= 2) 0.08 else 0.0
                val confidence = (nameScore * 0.85 + strengthBonus * 0.15 + multiBonus)
                    .coerceIn(0.0, 1.0)
                MedicineCandidate(med, confidence, ocrText.trim())
            }
            .filter { it.confidence > 0.30 }
            .sortedByDescending { it.confidence }
            .take(limit)
            .toList()
    }

    private fun bestTokenScore(query: List<String>, target: List<String>): Double {
        var best = 0.0
        for (q in query) for (t in target) {
            best = max(best, similarity(q, t))
            if (best == 1.0) return 1.0
        }
        return best
    }

    private fun strengthAgreement(strength: Pair<Double, String>?, med: Medicine): Double {
        if (strength == null || med.strengthValue == null) return 0.5
        val sameUnit = strength.second.equals(med.strengthUnit, ignoreCase = true)
        val sameValue = strength.first == med.strengthValue
        return when {
            sameUnit && sameValue -> 1.0
            sameValue -> 0.7
            else -> 0.0
        }
    }

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
