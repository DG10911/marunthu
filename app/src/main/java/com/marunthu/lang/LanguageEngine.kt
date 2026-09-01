package com.marunthu.lang

import com.marunthu.core.medicine.Substitute
import com.marunthu.core.model.ReasonCode
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.model.StructuredResult

/** Describes one supported output language. Add a language => add a LanguageProfile + templates. */
data class LanguageProfile(
    val code: String,          // "ta", "hi", "en"
    val displayName: String,   // "தமிழ்", "हिन्दी", "English"
    val ttsBcp47: String,      // "ta-IN", "hi-IN", "en-IN"
)

/** Fully-localized, ready-to-render + ready-to-speak message. */
data class LocalizedSafetyMessage(
    val title: String,
    val body: String,
    val spokenText: String,      // handed to TtsService
    val severityLabel: String,
)

/**
 * LEVEL 2 of the intelligence stack: deterministic localized templates. Always available,
 * always correct, never hallucinates. The optional on-device LLM (Sarvam-1, Level 3) only
 * *rephrases* this text — it can never change the medical verdict. The whole demo works on
 * this layer alone if the LLM is slow/unavailable.
 *
 * Design rule: the medical identity (drug names, ingredients) comes from the language-
 * independent [StructuredResult]; only the surrounding words change per language.
 */
object LanguageEngine {

    val SUPPORTED = listOf(
        LanguageProfile("ta", "தமிழ்", "ta-IN"),
        LanguageProfile("hi", "हिन्दी", "hi-IN"),
        LanguageProfile("en", "English", "en-IN"),
    )

    fun profile(code: String): LanguageProfile =
        SUPPORTED.firstOrNull { it.code == code } ?: SUPPORTED.last()

    fun explain(result: StructuredResult, lang: String): LocalizedSafetyMessage {
        val a = result.medicineA?.medicine
        val b = result.medicineB?.medicine
        val nameA = a?.brandName ?: "—"
        val nameB = b?.brandName ?: "—"
        val ingredient = result.sharedIngredient?.let { titleCase(it) }
            ?: a?.genericName ?: "—"

        return when (lang) {
            "ta" -> tamil(result, nameA, nameB, ingredient)
            "hi" -> hindi(result, nameA, nameB, ingredient)
            else -> english(result, nameA, nameB, ingredient)
        }
    }

    // ---- English ---------------------------------------------------------------
    private fun english(r: StructuredResult, a: String, b: String, ing: String) = when (r.reason) {
        ReasonCode.DUPLICATE_ACTIVE_INGREDIENT -> msg(
            "Same medicine",
            "$a and $b both contain $ing. Taking both together may be a double dose. " +
                "Please check with your doctor or pharmacist before taking both.",
            severity(r))
        ReasonCode.STRENGTH_DIFFERENCE -> msg(
            "Same ingredient, different strength",
            "$a and $b both contain $ing but at different strengths. " +
                "Please confirm the correct dose with your doctor or pharmacist.",
            severity(r))
        ReasonCode.POTENTIAL_INTERACTION -> msg(
            "Please check with a pharmacist",
            "$a and $b may interact. Please check with your doctor or pharmacist before taking both.",
            severity(r))
        ReasonCode.IDENTIFICATION_UNCERTAIN -> msg(
            "Not sure about this medicine",
            "We couldn't identify this medicine reliably. Please try another photo or confirm the name.",
            severity(r))
        ReasonCode.NO_ISSUE -> msg(
            "No problem found",
            "$a: no duplicate or known interaction was found in the prototype database.",
            severity(r))
    }

    // ---- Tamil -----------------------------------------------------------------
    private fun tamil(r: StructuredResult, a: String, b: String, ing: String) = when (r.reason) {
        ReasonCode.DUPLICATE_ACTIVE_INGREDIENT -> msg(
            "ஒரே மருந்து",
            "$a மற்றும் $b இரண்டிலும் ஒரே மருந்துப்பொருள் ($ing) உள்ளது. இரண்டையும் சேர்த்து " +
                "எடுத்துக்கொண்டால் அளவு இரட்டிப்பாகலாம். இரண்டையும் எடுப்பதற்கு முன் மருத்துவர் அல்லது " +
                "மருந்தாளரிடம் கேளுங்கள்.",
            severityTa(r))
        ReasonCode.STRENGTH_DIFFERENCE -> msg(
            "ஒரே பொருள், வெவ்வேறு அளவு",
            "$a மற்றும் $b இரண்டிலும் $ing உள்ளது, ஆனால் வெவ்வேறு அளவில். சரியான அளவை மருத்துவர் " +
                "அல்லது மருந்தாளரிடம் உறுதிசெய்யுங்கள்.",
            severityTa(r))
        ReasonCode.POTENTIAL_INTERACTION -> msg(
            "மருந்தாளரிடம் கேளுங்கள்",
            "$a மற்றும் $b ஒன்றுடன் ஒன்று பாதிப்பை ஏற்படுத்தலாம். இரண்டையும் எடுப்பதற்கு முன் " +
                "மருத்துவர் அல்லது மருந்தாளரிடம் கேளுங்கள்.",
            severityTa(r))
        ReasonCode.IDENTIFICATION_UNCERTAIN -> msg(
            "இந்த மருந்து தெளிவாக இல்லை",
            "இந்த மருந்தை சரியாக அடையாளம் காண முடியவில்லை. மற்றொரு படம் எடுக்கவும் அல்லது பெயரை " +
                "உறுதிசெய்யவும்.",
            severityTa(r))
        ReasonCode.NO_ISSUE -> msg(
            "பிரச்சனை எதுவும் இல்லை",
            "$a: முன்மாதிரி தரவுத்தளத்தில் நகல் அல்லது அறியப்பட்ட பாதிப்பு எதுவும் இல்லை.",
            severityTa(r))
    }

    // ---- Hindi -----------------------------------------------------------------
    private fun hindi(r: StructuredResult, a: String, b: String, ing: String) = when (r.reason) {
        ReasonCode.DUPLICATE_ACTIVE_INGREDIENT -> msg(
            "एक ही दवा",
            "$a और $b दोनों में एक ही घटक ($ing) है। दोनों को एक साथ लेने से खुराक दोगुनी हो सकती है। " +
                "दोनों लेने से पहले कृपया डॉक्टर या फार्मासिस्ट से पूछें।",
            severityHi(r))
        ReasonCode.STRENGTH_DIFFERENCE -> msg(
            "एक ही घटक, अलग मात्रा",
            "$a और $b दोनों में $ing है लेकिन अलग-अलग मात्रा में। सही खुराक डॉक्टर या फार्मासिस्ट से " +
                "पुष्टि करें।",
            severityHi(r))
        ReasonCode.POTENTIAL_INTERACTION -> msg(
            "फार्मासिस्ट से पूछें",
            "$a और $b एक-दूसरे पर असर डाल सकती हैं। दोनों लेने से पहले कृपया डॉक्टर या फार्मासिस्ट से पूछें।",
            severityHi(r))
        ReasonCode.IDENTIFICATION_UNCERTAIN -> msg(
            "यह दवा स्पष्ट नहीं है",
            "हम इस दवा को ठीक से पहचान नहीं सके। कृपया दूसरी फ़ोटो लें या नाम की पुष्टि करें।",
            severityHi(r))
        ReasonCode.NO_ISSUE -> msg(
            "कोई समस्या नहीं मिली",
            "$a: प्रोटोटाइप डेटाबेस में कोई डुप्लिकेट या ज्ञात इंटरैक्शन नहीं मिला।",
            severityHi(r))
    }

    /** Localized "same salt, cheaper" savings line. Always advises confirming with a pharmacist. */
    fun savingsLine(sub: Substitute, lang: String): String {
        val name = sub.medicine.brandName
        val price = sub.medicine.priceInr?.toInt() ?: 0
        val pct = sub.savingsPercent
        return when (lang) {
            "ta" -> "அதே மருந்து \"$name\" ₹$price-க்கு கிடைக்கிறது — $pct% சேமிக்கலாம். " +
                "மருந்தாளரிடம் கேட்டு வாங்குங்கள்."
            "hi" -> "वही दवा \"$name\" ₹$price में उपलब्ध है — $pct% की बचत। फार्मासिस्ट से पूछकर लें।"
            else -> "The same medicine is available as \"$name\" for ₹$price — save $pct%. " +
                "Ask your pharmacist."
        }
    }

    private fun msg(title: String, body: String, sev: String) =
        LocalizedSafetyMessage(title, body, spokenText = body, severityLabel = sev)

    private fun severity(r: StructuredResult) = when (r.status) {
        SafetyStatus.WARNING -> "Please check"
        SafetyStatus.UNCERTAIN -> "Not sure"
        SafetyStatus.OK -> "OK"
    }
    private fun severityTa(r: StructuredResult) = when (r.status) {
        SafetyStatus.WARNING -> "கவனம்"
        SafetyStatus.UNCERTAIN -> "உறுதியில்லை"
        SafetyStatus.OK -> "சரி"
    }
    private fun severityHi(r: StructuredResult) = when (r.status) {
        SafetyStatus.WARNING -> "ध्यान दें"
        SafetyStatus.UNCERTAIN -> "अनिश्चित"
        SafetyStatus.OK -> "ठीक"
    }

    private fun titleCase(s: String) =
        s.lowercase().replaceFirstChar { it.uppercase() }
}
