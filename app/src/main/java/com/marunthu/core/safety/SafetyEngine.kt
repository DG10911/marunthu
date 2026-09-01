package com.marunthu.core.safety

import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.model.ReasonCode
import com.marunthu.core.model.SafetyRule
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.model.Severity
import com.marunthu.core.model.StructuredResult

/**
 * DETERMINISTIC local safety engine. This is the medical authority of Marunthu — the
 * LLM never overrides it. Pure Kotlin, fully unit tested, zero network.
 *
 * Order of checks (first hit wins per pair):
 *   1. IDENTIFICATION_UNCERTAIN — any involved medicine below confidence gate
 *   2. DUPLICATE_ACTIVE_INGREDIENT — two meds share an active ingredient
 *   3. STRENGTH_DIFFERENCE — same ingredient, different strength (a softer duplicate)
 *   4. POTENTIAL_INTERACTION — a curated rule links two distinct ingredients
 *   5. NO_ISSUE
 */
class SafetyEngine(
    private val rules: List<SafetyRule>,
    private val confidenceGate: Double = 0.55,
) {

    /** Evaluate a single scanned medicine (identification confidence only). */
    fun evaluateSingle(candidate: MedicineCandidate): StructuredResult =
        if (candidate.confidence < confidenceGate) {
            uncertain(candidate)
        } else {
            StructuredResult(
                status = SafetyStatus.OK,
                reason = ReasonCode.NO_ISSUE,
                severity = Severity.LOW,
                medicineA = candidate,
                medicineB = null,
                confidence = candidate.confidence,
            )
        }

    /**
     * Evaluate every pair among the scanned medicines and return the most severe
     * finding first. Empty pairing (0 or 1 med) delegates to single evaluation.
     */
    fun evaluate(candidates: List<MedicineCandidate>): List<StructuredResult> {
        if (candidates.isEmpty()) return emptyList()
        if (candidates.size == 1) return listOf(evaluateSingle(candidates.first()))

        val results = mutableListOf<StructuredResult>()
        for (i in candidates.indices) for (j in i + 1 until candidates.size) {
            results += evaluatePair(candidates[i], candidates[j])
        }
        return results.sortedWith(
            compareByDescending<StructuredResult> { it.status == SafetyStatus.WARNING }
                .thenByDescending { it.severity.ordinal }
        )
    }

    private fun evaluatePair(a: MedicineCandidate, b: MedicineCandidate): StructuredResult {
        val minConf = minOf(a.confidence, b.confidence)
        if (minConf < confidenceGate) return uncertain(if (a.confidence < b.confidence) a else b)

        val shared = a.ingredientIds.firstOrNull { it in b.ingredientIds }
        if (shared != null) {
            // Strength is only comparable when BOTH medicines are single-ingredient — then the
            // per-medicine strength refers to the shared ingredient. For combination drugs the
            // listed strength refers to some other component, so we must not use it to soften a
            // genuine duplicate: default to DUPLICATE (the safe, meaningful warning).
            val bothSingleIngredient =
                a.medicine.ingredientIds.size == 1 && b.medicine.ingredientIds.size == 1
            val sameStrength = a.medicine.strengthValue == b.medicine.strengthValue &&
                a.medicine.strengthUnit == b.medicine.strengthUnit
            val strengthDiff = bothSingleIngredient && !sameStrength
            return StructuredResult(
                status = SafetyStatus.WARNING,
                reason = if (strengthDiff) ReasonCode.STRENGTH_DIFFERENCE
                         else ReasonCode.DUPLICATE_ACTIVE_INGREDIENT,
                severity = if (strengthDiff) Severity.MODERATE else Severity.HIGH,
                medicineA = a, medicineB = b,
                sharedIngredient = shared,
                confidence = minConf,
                ruleSource = "derived: shared active ingredient",
            )
        }

        val rule = findInteraction(a.ingredientIds, b.ingredientIds)
        if (rule != null) {
            return StructuredResult(
                status = SafetyStatus.WARNING,
                reason = ReasonCode.POTENTIAL_INTERACTION,
                severity = rule.severity,
                medicineA = a, medicineB = b,
                confidence = minConf,
                ruleSource = rule.source,
            )
        }

        return StructuredResult(
            status = SafetyStatus.OK,
            reason = ReasonCode.NO_ISSUE,
            severity = Severity.LOW,
            medicineA = a, medicineB = b,
            confidence = minConf,
        )
    }

    private fun findInteraction(idsA: List<String>, idsB: List<String>): SafetyRule? =
        rules.firstOrNull { r ->
            r.kind == ReasonCode.POTENTIAL_INTERACTION &&
                ((r.ingredientA in idsA && r.ingredientB in idsB) ||
                 (r.ingredientA in idsB && r.ingredientB in idsA))
        }

    private fun uncertain(c: MedicineCandidate) = StructuredResult(
        status = SafetyStatus.UNCERTAIN,
        reason = ReasonCode.IDENTIFICATION_UNCERTAIN,
        severity = Severity.LOW,
        medicineA = c, medicineB = null,
        confidence = c.confidence,
    )
}
