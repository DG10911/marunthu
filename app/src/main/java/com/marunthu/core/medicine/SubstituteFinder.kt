package com.marunthu.core.medicine

import com.marunthu.core.model.Medicine

/** A cheaper same-composition alternative to a scanned medicine. */
data class Substitute(
    val medicine: Medicine,
    val savingsInr: Double,
    val savingsPercent: Int,
)

/**
 * Finds cheaper medicines with the EXACT SAME active-ingredient set + strength — the
 * "same salt, lower price" idea Indians use to save 50–90% (Jan Aushadhi / generics).
 * Pure Kotlin, fully offline, unit tested. It compares composition, never "recommends" a
 * switch — the UI always says to confirm with a pharmacist.
 */
class SubstituteFinder(private val catalog: List<Medicine>) {

    /** @return cheaper same-composition options, biggest saving first. */
    fun cheaperAlternatives(med: Medicine, limit: Int = 3): List<Substitute> {
        val price = med.priceInr ?: return emptyList()
        val key = compositionKey(med)
        return catalog.asSequence()
            .filter { it.canonicalId != med.canonicalId }
            .filter { compositionKey(it) == key }
            .filter { (it.priceInr ?: Double.MAX_VALUE) < price }
            .map { alt ->
                val saving = price - alt.priceInr!!
                Substitute(alt, saving, ((saving / price) * 100).toInt())
            }
            .sortedByDescending { it.savingsInr }
            .take(limit)
            .toList()
    }

    /** Same ingredients (order-independent) + same strength = interchangeable composition. */
    private fun compositionKey(m: Medicine): String {
        val ings = m.ingredientIds.sorted().joinToString("+")
        val strength = "${m.strengthValue}${m.strengthUnit}"
        return "$ings@$strength"
    }
}
