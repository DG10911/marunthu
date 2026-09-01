package com.marunthu.lang

import com.marunthu.core.medicine.Substitute
import com.marunthu.core.model.ReasonCode
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.model.StructuredResult

/** Describes one supported output language. Add a language => add a column to the maps below. */
data class LanguageProfile(val code: String, val displayName: String, val ttsBcp47: String)

/** Fully-localized, ready-to-render + ready-to-speak message. */
data class LocalizedSafetyMessage(
    val title: String,
    val body: String,
    val spokenText: String,
    val severityLabel: String,
)

/**
 * LEVEL 2 of the intelligence stack: deterministic localized templates. Data-driven so new
 * languages are just new map entries; any missing (language, reason) gracefully falls back to
 * English. Medical identity (names/ingredients) comes from the language-independent
 * [StructuredResult]; only the surrounding words change.
 */
object LanguageEngine {

    val SUPPORTED = listOf(
        LanguageProfile("ta", "தமிழ்", "ta-IN"),
        LanguageProfile("hi", "हिन्दी", "hi-IN"),
        LanguageProfile("en", "English", "en-IN"),
        LanguageProfile("te", "తెలుగు", "te-IN"),
        LanguageProfile("kn", "ಕನ್ನಡ", "kn-IN"),
        LanguageProfile("ml", "മലയാളം", "ml-IN"),
    )

    fun profile(code: String): LanguageProfile =
        SUPPORTED.firstOrNull { it.code == code } ?: SUPPORTED.first { it.code == "en" }

    fun explain(result: StructuredResult, lang: String): LocalizedSafetyMessage {
        val a = result.medicineA?.medicine?.brandName ?: "—"
        val b = result.medicineB?.medicine?.brandName ?: "—"
        val ing = result.sharedIngredient?.let { titleCase(it) }
            ?: result.medicineA?.medicine?.genericName ?: "—"
        val title = pick(TITLE, lang, result.reason)
        val body = pick(BODY, lang, result.reason)
            .replace("{A}", a).replace("{B}", b).replace("{ING}", ing)
        val sev = SEV[lang] ?: SEV["en"]!!
        val label = when (result.status) {
            SafetyStatus.WARNING -> sev.first
            SafetyStatus.UNCERTAIN -> sev.second
            SafetyStatus.OK -> sev.third
        }
        return LocalizedSafetyMessage(title, body, spokenText = body, severityLabel = label)
    }

    /** Localized "same salt, cheaper" savings line. */
    fun savingsLine(sub: Substitute, lang: String): String {
        val tmpl = SAVE[lang] ?: SAVE["en"]!!
        return tmpl.replace("{name}", sub.medicine.brandName)
            .replace("{price}", (sub.medicine.priceInr?.toInt() ?: 0).toString())
            .replace("{pct}", sub.savingsPercent.toString())
    }

    private fun pick(m: Map<String, Map<ReasonCode, String>>, lang: String, r: ReasonCode): String =
        m[lang]?.get(r) ?: m["en"]!!.getValue(r)

    private fun titleCase(s: String) = s.lowercase().replaceFirstChar { it.uppercase() }

    // ---- TITLES: lang -> reason -> title ----
    private val TITLE: Map<String, Map<ReasonCode, String>> = mapOf(
        "en" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "Same medicine",
            ReasonCode.STRENGTH_DIFFERENCE to "Same ingredient, different strength",
            ReasonCode.POTENTIAL_INTERACTION to "Please check with a pharmacist",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "Not sure about this medicine",
            ReasonCode.NO_ISSUE to "No problem found",
        ),
        "ta" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "ஒரே மருந்து",
            ReasonCode.STRENGTH_DIFFERENCE to "ஒரே பொருள், வெவ்வேறு அளவு",
            ReasonCode.POTENTIAL_INTERACTION to "மருந்தாளரிடம் கேளுங்கள்",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "இந்த மருந்து தெளிவாக இல்லை",
            ReasonCode.NO_ISSUE to "பிரச்சனை எதுவும் இல்லை",
        ),
        "hi" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "एक ही दवा",
            ReasonCode.STRENGTH_DIFFERENCE to "एक ही घटक, अलग मात्रा",
            ReasonCode.POTENTIAL_INTERACTION to "फार्मासिस्ट से पूछें",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "यह दवा स्पष्ट नहीं है",
            ReasonCode.NO_ISSUE to "कोई समस्या नहीं मिली",
        ),
        "te" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "ఒకటే మందు",
            ReasonCode.STRENGTH_DIFFERENCE to "ఒకే పదార్థం, వేరే మోతాదు",
            ReasonCode.POTENTIAL_INTERACTION to "ఫార్మసిస్ట్‌ను అడగండి",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ఈ మందు స్పష్టంగా లేదు",
            ReasonCode.NO_ISSUE to "సమస్య ఏమీ లేదు",
        ),
        "kn" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "ಒಂದೇ ಔಷಧಿ",
            ReasonCode.STRENGTH_DIFFERENCE to "ಒಂದೇ ಘಟಕ, ಬೇರೆ ಪ್ರಮಾಣ",
            ReasonCode.POTENTIAL_INTERACTION to "ಔಷಧಿಕಾರರನ್ನು ಕೇಳಿ",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ಈ ಔಷಧಿ ಸ್ಪಷ್ಟವಾಗಿಲ್ಲ",
            ReasonCode.NO_ISSUE to "ಸಮಸ್ಯೆ ಇಲ್ಲ",
        ),
        "ml" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "ഒരേ മരുന്ന്",
            ReasonCode.STRENGTH_DIFFERENCE to "ഒരേ ഘടകം, വ്യത്യസ്ത അളവ്",
            ReasonCode.POTENTIAL_INTERACTION to "ഫാർമസിസ്റ്റിനോട് ചോദിക്കുക",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ഈ മരുന്ന് വ്യക്തമല്ല",
            ReasonCode.NO_ISSUE to "പ്രശ്നമില്ല",
        ),
    )

    // ---- BODIES: lang -> reason -> body (with {A} {B} {ING} placeholders) ----
    private val BODY: Map<String, Map<ReasonCode, String>> = mapOf(
        "en" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A} and {B} both contain {ING}. Taking both together may be a double dose. Please check with your doctor or pharmacist before taking both.",
            ReasonCode.STRENGTH_DIFFERENCE to "{A} and {B} both contain {ING} but at different strengths. Please confirm the correct dose with your doctor or pharmacist.",
            ReasonCode.POTENTIAL_INTERACTION to "{A} and {B} may interact. Please check with your doctor or pharmacist before taking both.",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "We couldn't identify this medicine reliably. Please try another photo or confirm the name.",
            ReasonCode.NO_ISSUE to "{A}: no duplicate or known interaction was found in the prototype database.",
        ),
        "ta" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A} மற்றும் {B} இரண்டிலும் ஒரே மருந்துப்பொருள் ({ING}) உள்ளது. இரண்டையும் சேர்த்து எடுத்துக்கொண்டால் அளவு இரட்டிப்பாகலாம். இரண்டையும் எடுப்பதற்கு முன் மருத்துவர் அல்லது மருந்தாளரிடம் கேளுங்கள்.",
            ReasonCode.STRENGTH_DIFFERENCE to "{A} மற்றும் {B} இரண்டிலும் {ING} உள்ளது, ஆனால் வெவ்வேறு அளவில். சரியான அளவை மருத்துவர் அல்லது மருந்தாளரிடம் உறுதிசெய்யுங்கள்.",
            ReasonCode.POTENTIAL_INTERACTION to "{A} மற்றும் {B} ஒன்றுடன் ஒன்று பாதிப்பை ஏற்படுத்தலாம். இரண்டையும் எடுப்பதற்கு முன் மருத்துவர் அல்லது மருந்தாளரிடம் கேளுங்கள்.",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "இந்த மருந்தை சரியாக அடையாளம் காண முடியவில்லை. மற்றொரு படம் எடுக்கவும் அல்லது பெயரை உறுதிசெய்யவும்.",
            ReasonCode.NO_ISSUE to "{A}: முன்மாதிரி தரவுத்தளத்தில் நகல் அல்லது அறியப்பட்ட பாதிப்பு எதுவும் இல்லை.",
        ),
        "hi" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A} और {B} दोनों में एक ही घटक ({ING}) है। दोनों को एक साथ लेने से खुराक दोगुनी हो सकती है। दोनों लेने से पहले कृपया डॉक्टर या फार्मासिस्ट से पूछें।",
            ReasonCode.STRENGTH_DIFFERENCE to "{A} और {B} दोनों में {ING} है लेकिन अलग-अलग मात्रा में। सही खुराक डॉक्टर या फार्मासिस्ट से पुष्टि करें।",
            ReasonCode.POTENTIAL_INTERACTION to "{A} और {B} एक-दूसरे पर असर डाल सकती हैं। दोनों लेने से पहले कृपया डॉक्टर या फार्मासिस्ट से पूछें।",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "हम इस दवा को ठीक से पहचान नहीं सके। कृपया दूसरी फ़ोटो लें या नाम की पुष्टि करें।",
            ReasonCode.NO_ISSUE to "{A}: प्रोटोटाइप डेटाबेस में कोई डुप्लिकेट या ज्ञात इंटरैक्शन नहीं मिला।",
        ),
        "te" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A} మరియు {B} రెండింటిలోనూ {ING} ఉంది. రెండింటినీ కలిపి తీసుకుంటే మోతాదు రెట్టింపు కావచ్చు. రెండింటినీ తీసుకునే ముందు వైద్యుడిని లేదా ఫార్మసిస్ట్‌ను అడగండి.",
            ReasonCode.STRENGTH_DIFFERENCE to "{A} మరియు {B} రెండింటిలోనూ {ING} ఉంది కానీ వేరే మోతాదులో. సరైన మోతాదును వైద్యుడిని అడిగి నిర్ధారించుకోండి.",
            ReasonCode.POTENTIAL_INTERACTION to "{A} మరియు {B} ఒకదానిపై ఒకటి ప్రభావం చూపవచ్చు. రెండింటినీ తీసుకునే ముందు వైద్యుడిని లేదా ఫార్మసిస్ట్‌ను అడగండి.",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ఈ మందును సరిగ్గా గుర్తించలేకపోయాము. మరో ఫోటో తీయండి లేదా పేరును నిర్ధారించండి.",
            ReasonCode.NO_ISSUE to "{A}: నమూనా డేటాబేస్‌లో నకిలీ లేదా తెలిసిన ప్రభావం ఏదీ కనబడలేదు.",
        ),
        "kn" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A} ಮತ್ತು {B} ಎರಡರಲ್ಲೂ {ING} ಇದೆ. ಎರಡನ್ನೂ ಜೊತೆಗೆ ತೆಗೆದುಕೊಂಡರೆ ಪ್ರಮಾಣ ದ್ವಿಗುಣವಾಗಬಹುದು. ಎರಡನ್ನೂ ತೆಗೆದುಕೊಳ್ಳುವ ಮೊದಲು ವೈದ್ಯರನ್ನು ಅಥವಾ ಔಷಧಿಕಾರರನ್ನು ಕೇಳಿ.",
            ReasonCode.STRENGTH_DIFFERENCE to "{A} ಮತ್ತು {B} ಎರಡರಲ್ಲೂ {ING} ಇದೆ ಆದರೆ ಬೇರೆ ಪ್ರಮಾಣದಲ್ಲಿ. ಸರಿಯಾದ ಪ್ರಮಾಣವನ್ನು ವೈದ್ಯರಿಂದ ಖಚಿತಪಡಿಸಿಕೊಳ್ಳಿ.",
            ReasonCode.POTENTIAL_INTERACTION to "{A} ಮತ್ತು {B} ಒಂದರ ಮೇಲೊಂದು ಪರಿಣಾಮ ಬೀರಬಹುದು. ಎರಡನ್ನೂ ತೆಗೆದುಕೊಳ್ಳುವ ಮೊದಲು ವೈದ್ಯರನ್ನು ಅಥವಾ ಔಷಧಿಕಾರರನ್ನು ಕೇಳಿ.",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ಈ ಔಷಧಿಯನ್ನು ಸರಿಯಾಗಿ ಗುರುತಿಸಲಾಗಲಿಲ್ಲ. ಇನ್ನೊಂದು ಫೋಟೋ ತೆಗೆಯಿರಿ ಅಥವಾ ಹೆಸರನ್ನು ಖಚಿತಪಡಿಸಿ.",
            ReasonCode.NO_ISSUE to "{A}: ಮಾದರಿ ಡೇಟಾಬೇಸ್‌ನಲ್ಲಿ ನಕಲಿ ಅಥವಾ ತಿಳಿದ ಪರಿಣಾಮ ಯಾವುದೂ ಸಿಗಲಿಲ್ಲ.",
        ),
        "ml" to mapOf(
            ReasonCode.DUPLICATE_ACTIVE_INGREDIENT to "{A}, {B} എന്നിവയിൽ രണ്ടിലും {ING} ഉണ്ട്. രണ്ടും ഒരുമിച്ച് കഴിച്ചാൽ അളവ് ഇരട്ടിയാകാം. രണ്ടും കഴിക്കുന്നതിന് മുമ്പ് ഡോക്ടറോട് അല്ലെങ്കിൽ ഫാർമസിസ്റ്റിനോട് ചോദിക്കുക.",
            ReasonCode.STRENGTH_DIFFERENCE to "{A}, {B} എന്നിവയിൽ {ING} ഉണ്ട് പക്ഷേ വ്യത്യസ്ത അളവിൽ. ശരിയായ അളവ് ഡോക്ടറോട് ഉറപ്പാക്കുക.",
            ReasonCode.POTENTIAL_INTERACTION to "{A}, {B} എന്നിവ പരസ്പരം ബാധിച്ചേക്കാം. രണ്ടും കഴിക്കുന്നതിന് മുമ്പ് ഡോക്ടറോട് അല്ലെങ്കിൽ ഫാർമസിസ്റ്റിനോട് ചോദിക്കുക.",
            ReasonCode.IDENTIFICATION_UNCERTAIN to "ഈ മരുന്ന് ശരിയായി തിരിച്ചറിയാൻ കഴിഞ്ഞില്ല. മറ്റൊരു ഫോട്ടോ എടുക്കുക അല്ലെങ്കിൽ പേര് ഉറപ്പാക്കുക.",
            ReasonCode.NO_ISSUE to "{A}: മാതൃകാ ഡാറ്റാബേസിൽ തനിപ്പകർപ്പോ അറിയപ്പെടുന്ന പ്രതിപ്രവർത്തനമോ കണ്ടെത്തിയില്ല.",
        ),
    )

    // ---- severity labels: lang -> (warning, uncertain, ok) ----
    private val SEV: Map<String, Triple<String, String, String>> = mapOf(
        "en" to Triple("Please check", "Not sure", "OK"),
        "ta" to Triple("கவனம்", "உறுதியில்லை", "சரி"),
        "hi" to Triple("ध्यान दें", "अनिश्चित", "ठीक"),
        "te" to Triple("గమనిక", "ఖచ్చితంగా తెలియదు", "సరే"),
        "kn" to Triple("ಗಮನಿಸಿ", "ಖಚಿತವಿಲ್ಲ", "ಸರಿ"),
        "ml" to Triple("ശ്രദ്ധിക്കുക", "ഉറപ്പില്ല", "ശരി"),
    )

    // ---- savings line: lang -> template with {name} {price} {pct} ----
    private val SAVE: Map<String, String> = mapOf(
        "en" to "The same medicine is available as \"{name}\" for ₹{price} — save {pct}%. Ask your pharmacist.",
        "ta" to "அதே மருந்து \"{name}\" ₹{price}-க்கு கிடைக்கிறது — {pct}% சேமிக்கலாம். மருந்தாளரிடம் கேட்டு வாங்குங்கள்.",
        "hi" to "वही दवा \"{name}\" ₹{price} में उपलब्ध है — {pct}% की बचत। फार्मासिस्ट से पूछकर लें।",
        "te" to "అదే మందు \"{name}\" ₹{price}కి లభిస్తుంది — {pct}% ఆదా. ఫార్మసిస్ట్‌ను అడిగి కొనండి.",
        "kn" to "ಅದೇ ಔಷಧಿ \"{name}\" ₹{price}ಗೆ ಸಿಗುತ್ತದೆ — {pct}% ಉಳಿತಾಯ. ಔಷಧಿಕಾರರನ್ನು ಕೇಳಿ ಖರೀದಿಸಿ.",
        "ml" to "അതേ മരുന്ന് \"{name}\" ₹{price}ന് ലഭ്യമാണ് — {pct}% ലാഭം. ഫാർമസിസ്റ്റിനോട് ചോദിച്ച് വാങ്ങുക.",
    )
}
