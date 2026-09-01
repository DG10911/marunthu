package com.marunthu.core.ocr

/**
 * Cleans raw ML Kit OCR output into a normalized token stream for medicine matching.
 * Pure Kotlin, no Android — unit tested.
 *
 * Medicine strips print brand/generic names + strengths in LATIN script, which is why
 * we can rely on offline Latin OCR and never need on-device Tamil-script OCR.
 */
object TextNormalizer {

    private val NOISE = setOf(
        "tablet", "tablets", "tab", "cap", "capsule", "capsules", "mg", "ml", "mcg",
        "gm", "g", "ip", "bp", "usp", "each", "contains", "composition", "store",
        "keep", "away", "children", "reach", "batch", "mfg", "exp", "rs", "mrp",
    )

    /** Uppercased, punctuation-stripped, whitespace-collapsed single line. */
    fun normalize(raw: String): String =
        raw.uppercase()
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Content tokens (>=3 chars, non-noise, non-pure-number) for fuzzy matching. */
    fun tokens(raw: String): List<String> =
        normalize(raw).lowercase().split(" ")
            .filter { it.length >= 3 && it !in NOISE && !it.all { c -> c.isDigit() } }
            .distinct()

    /** Extracts a strength like 500 / 500MG / 5 MG → Pair(value, unit) if present. */
    fun extractStrength(raw: String): Pair<Double, String>? {
        val m = Regex("(\\d{1,4}(?:\\.\\d+)?)\\s*(MG|MCG|ML|G|IU)", RegexOption.IGNORE_CASE)
            .find(raw) ?: return null
        val value = m.groupValues[1].toDoubleOrNull() ?: return null
        return value to m.groupValues[2].uppercase()
    }
}
