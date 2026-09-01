package com.marunthu.core.expiry

/** Result of reading an expiry (EXP) date off a medicine strip via OCR. */
data class ExpiryInfo(
    val month: Int,
    val year: Int,           // 4-digit
    val expired: Boolean,
    val expiringSoon: Boolean,   // within EXPIRING_SOON_MONTHS and not yet expired
    val monthsLeft: Int,         // negative if expired
    val text: String,            // matched raw span, e.g. "08/2025"
)

/**
 * Extracts and evaluates the printed EXP date from raw OCR text. Pure Kotlin — unit tested.
 * Indian strips print things like "EXP 08/25", "EXP. 08/2025", "USE BEFORE 08/2026".
 * MFG dates are ignored (we only key off EXP / USE BEFORE / EXPIRY).
 */
object ExpiryParser {

    const val EXPIRING_SOON_MONTHS = 3

    // EXP / EXPIRY / USE BEFORE / USE BY, then MM/YY or MM/YYYY (slash, dash or space)
    private val EXP = Regex(
        """(?:EXP(?:IRY)?|USE\s*(?:BEFORE|BY))\.?\s*:?\s*(\d{1,2})\s*[/\-. ]\s*(\d{2,4})""",
        RegexOption.IGNORE_CASE,
    )

    /** @return evaluated expiry, or null if no EXP date is found. */
    fun parse(ocrText: String, nowYear: Int, nowMonth: Int): ExpiryInfo? {
        val m = EXP.find(ocrText) ?: return null
        var mm = m.groupValues[1].toIntOrNull() ?: return null
        var yy = m.groupValues[2].toIntOrNull() ?: return null
        if (mm !in 1..12) return null
        if (yy < 100) yy += 2000   // 25 -> 2025
        val expIndex = yy * 12 + mm
        val nowIndex = nowYear * 12 + nowMonth
        val monthsLeft = expIndex - nowIndex
        return ExpiryInfo(
            month = mm, year = yy,
            expired = monthsLeft < 0,
            expiringSoon = monthsLeft in 0..EXPIRING_SOON_MONTHS,
            monthsLeft = monthsLeft,
            text = "%02d/%d".format(mm, yy),
        )
    }

    /** Active ingredients that become HARMFUL (not just weaker) once expired. */
    private val TOXIC_WHEN_EXPIRED = setOf(
        "TETRACYCLINE", "DOXYCYCLINE", "MINOCYCLINE", "OXYTETRACYCLINE",
    )

    fun becomesToxicWhenExpired(ingredientIds: List<String>): Boolean =
        ingredientIds.any { it in TOXIC_WHEN_EXPIRED }
}
