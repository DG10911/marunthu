package com.marunthu.core.medicine

import com.marunthu.core.model.Medicine
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.ocr.TextNormalizer
import kotlin.math.max
import kotlin.math.min

/**
 * Resolves noisy OCR text to ranked [MedicineCandidate]s against the local catalog.
 * Pure Kotlin — unit tested.
 *
 * Two matching signals, whichever is stronger:
 *  1. BRAND fuzzy match (tolerant of OCR typos) via a 3-char prefix index.
 *  2. COMPOSITION match — Indian strips always print the salt list ("Paracetamol +
 *     Phenylephrine + Chlorpheniramine"), which OCR reads cleanly. We detect ingredient
 *     names in the text and identify the medicine by its exact composition. This is far more
 *     reliable than the stylised brand logo, and it means we can flag safety even when the
 *     brand isn't captured.
 *
 * Crucially it does NOT force a confident guess: weak matches score low so the UI can say
 * "couldn't identify — try again" instead of returning a wrong medicine.
 */
class MedicineMatcher(private val catalog: List<Medicine>) {

    private val brandIndex = HashMap<String, MutableList<Int>>()      // token prefix -> med idx
    private val ingredientNames: List<String>                        // distinct ingredient ids
    private val ingredientIndex = HashMap<String, MutableList<Int>>() // prefix -> ingredientNames idx
    private val compositionIndex = HashMap<String, MutableList<Int>>()// sorted salt key -> med idx

    init {
        catalog.forEachIndexed { idx, med ->
            med.searchableTokens.forEach { tok ->
                if (tok.length >= 3) brandIndex.getOrPut(tok.substring(0, 3)) { ArrayList() }.add(idx)
            }
            if (med.ingredientIds.isNotEmpty())
                compositionIndex.getOrPut(compKey(med.ingredientIds)) { ArrayList() }.add(idx)
        }
        val ings = LinkedHashSet<String>()
        catalog.forEach { ings.addAll(it.ingredientIds) }
        ingredientNames = ings.toList()
        ingredientNames.forEachIndexed { i, name ->
            val n = name.lowercase()
            if (n.length >= 3) ingredientIndex.getOrPut(n.substring(0, 3)) { ArrayList() }.add(i)
        }
    }

    private fun compKey(ings: List<String>) = ings.sorted().joinToString("+")

    /** Ingredient ids whose names appear (fuzzily) in the OCR tokens. */
    private fun detectIngredients(tokens: List<String>): Set<String> {
        val found = LinkedHashSet<String>()
        for (t in tokens) {
            if (t.length < 4) continue
            ingredientIndex[t.substring(0, 3)]?.forEach { i ->
                val name = ingredientNames[i]
                if (similarity(t, name.lowercase()) >= 0.82) found.add(name)
            }
        }
        return found
    }

    /** @return candidates sorted by descending confidence, best first. */
    fun match(ocrText: String, limit: Int = 5): List<MedicineCandidate> {
        val queryTokens = TextNormalizer.tokens(ocrText)
        if (queryTokens.isEmpty()) return emptyList()
        val strength = TextNormalizer.extractStrength(ocrText)
        val detected = detectIngredients(queryTokens)

        val candidates = LinkedHashSet<Int>()
        for (q in queryTokens) if (q.length >= 3) brandIndex[q.substring(0, 3)]?.let { candidates.addAll(it) }
        if (detected.isNotEmpty()) compositionIndex[compKey(detected.toList())]?.let { candidates.addAll(it) }

        val pool: Iterable<Int> = when {
            candidates.isNotEmpty() -> candidates
            catalog.size <= 500 -> catalog.indices
            else -> return emptyList()
        }

        return pool.asSequence()
            .map { catalog[it] }
            .map { med ->
                val brand = bestTokenScore(queryTokens, med.searchableTokens)
                val strengthBonus = strengthAgreement(strength, med)
                val strongMatches = med.searchableTokens.count { t ->
                    t.length >= 4 && queryTokens.any { q -> similarity(q, t) >= 0.8 }
                }
                val multiBonus = if (strongMatches >= 2) 0.08 else 0.0
                val brandConf = (brand * 0.85 + strengthBonus * 0.15 + multiBonus).coerceIn(0.0, 1.0)

                val compConf = if (med.ingredientIds.isNotEmpty() && detected.isNotEmpty()) {
                    val hits = med.ingredientIds.count { it in detected }
                    val coverage = hits.toDouble() / med.ingredientIds.size
                    val exact = coverage == 1.0 && med.ingredientIds.size == detected.size
                    when {
                        exact -> 0.92
                        coverage == 1.0 -> 0.82   // all salts present (+ extra text detected)
                        hits >= 2 -> 0.6 + 0.1 * coverage
                        else -> 0.0
                    }
                } else 0.0

                MedicineCandidate(med, max(brandConf, compConf), ocrText.trim())
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
