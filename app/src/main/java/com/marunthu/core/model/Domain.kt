package com.marunthu.core.model

/**
 * CANONICAL, LANGUAGE-INDEPENDENT domain model.
 *
 * Nothing in this file knows about Tamil, Hindi, English, TTS, Android, or the LLM.
 * The medical identity of a drug is the same in every language; only the *presentation*
 * layer (com.marunthu.lang) ever attaches human words. This separation is the core
 * architectural principle of Marunthu.
 */

/** A pharmacological active ingredient, e.g. METFORMIN. */
data class Ingredient(
    val id: String,                 // canonical, e.g. "METFORMIN"
    val canonicalName: String,      // "Metformin"
    val aliases: List<String> = emptyList(),
)

/** A concrete marketed product, e.g. a strip of Glycomet 500. */
data class Medicine(
    val canonicalId: String,        // "GLYCOMET_500_TAB"
    val brandName: String,          // "Glycomet"
    val genericName: String,        // "Metformin"
    val ingredientIds: List<String>,// canonical ingredient ids (combos have >1)
    val strengthValue: Double?,     // 500.0
    val strengthUnit: String?,      // "MG"
    val dosageForm: String,         // "TABLET"
    val manufacturer: String = "",
    val aliases: List<String> = emptyList(), // extra brand spellings / pack text
    val priceInr: Double? = null,            // pack price in ₹, for generic-savings comparison
    val isGeneric: Boolean = false,          // true for Jan Aushadhi / unbranded generics
) {
    /** Lowercased token bag used for fuzzy matching against OCR text. */
    val searchableTokens: List<String> by lazy {
        (listOf(brandName, genericName) + aliases)
            .flatMap { it.lowercase().split(Regex("[^a-z0-9]+")) }
            .filter { it.length >= 3 }
            .distinct()
    }
}

/** A ranked hypothesis that some OCR text corresponds to a known Medicine. */
data class MedicineCandidate(
    val medicine: Medicine,
    val confidence: Double,         // 0.0 .. 1.0
    val matchedText: String,        // the raw OCR span that produced this match
) {
    val canonicalId get() = medicine.canonicalId
    val ingredientIds get() = medicine.ingredientIds
}

enum class Severity { LOW, MODERATE, HIGH }

/**
 * Machine-readable reason codes. The language layer maps each code to a localized
 * template. NEVER put user-facing prose here.
 */
enum class ReasonCode {
    NO_ISSUE,
    DUPLICATE_ACTIVE_INGREDIENT,
    POTENTIAL_INTERACTION,
    STRENGTH_DIFFERENCE,
    IDENTIFICATION_UNCERTAIN,
}

enum class SafetyStatus { OK, WARNING, UNCERTAIN }

/** A curated, prototype-labelled interaction/duplicate rule. */
data class SafetyRule(
    val ruleId: String,
    val ingredientA: String,
    val ingredientB: String,
    val severity: Severity,
    val kind: ReasonCode,           // POTENTIAL_INTERACTION (duplicates are derived, not stored)
    val source: String = "Marunthu prototype dataset",
)

/**
 * The structured, language-independent verdict. This is the ONLY thing handed to the
 * language/LLM layer. The LLM phrases it; it never decides it.
 */
data class StructuredResult(
    val status: SafetyStatus,
    val reason: ReasonCode,
    val severity: Severity,
    val medicineA: MedicineCandidate?,
    val medicineB: MedicineCandidate?,
    val sharedIngredient: String? = null,   // canonical ingredient id, if relevant
    val confidence: Double,                 // min confidence of the involved candidates
    val ruleSource: String? = null,
)
