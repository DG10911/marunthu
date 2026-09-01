package com.marunthu.data

/** One piece of localized advice about a medicine (icon + text). */
data class Advisory(val icon: String, val text: String)

/**
 * Curated, prototype safety advice per active ingredient: food timing, alcohol interaction,
 * and pregnancy caution. Facts about the DRUG (not the user), so no profile toggle needed.
 * Tamil / Hindi / English; missing language falls back to English. Conservative on purpose —
 * only well-established cautions are listed, and everything is prefixed "ask your doctor".
 */
object MedicineAdvisor {

    // ingredient -> lang -> "when to take / why"
    private val FOOD: Map<String, Map<String, String>> = mapOf(
        "METFORMIN" to l("Take with food — avoids stomach upset and loose motions.",
            "உணவுடன் சாப்பிடுங்கள் — வயிற்றுக் கோளாறைத் தவிர்க்கும்.",
            "खाने के साथ लें — पेट खराब होने से बचाता है।"),
        "AZITHROMYCIN" to l("Take 1 hour before or 2 hours after food.",
            "உணவுக்கு 1 மணி நேரம் முன் அல்லது 2 மணி நேரம் பின் சாப்பிடுங்கள்.",
            "खाने से 1 घंटा पहले या 2 घंटे बाद लें।"),
        "IBUPROFEN" to l("Take with food to protect your stomach.",
            "வயிற்றைப் பாதுகாக்க உணவுடன் சாப்பிடுங்கள்.",
            "पेट की सुरक्षा के लिए खाने के साथ लें।"),
        "ACECLOFENAC" to l("Take with food to protect your stomach.",
            "வயிற்றைப் பாதுகாக்க உணவுடன் சாப்பிடுங்கள்.",
            "पेट की सुरक्षा के लिए खाने के साथ लें।"),
        "ASPIRIN" to l("Take with food to reduce stomach irritation.",
            "வயிற்று எரிச்சலைக் குறைக்க உணவுடன் சாப்பிடுங்கள்.",
            "पेट की जलन कम करने के लिए खाने के साथ लें।"),
        "LEVOTHYROXINE" to l("Take on an empty stomach, 30–60 min before breakfast.",
            "காலை உணவுக்கு 30–60 நிமிடம் முன், வெறும் வயிற்றில் எடுங்கள்.",
            "खाली पेट, नाश्ते से 30–60 मिनट पहले लें।"),
        "PANTOPRAZOLE" to l("Take before food, usually in the morning.",
            "உணவுக்கு முன், பொதுவாக காலையில் எடுங்கள்.",
            "खाने से पहले, आमतौर पर सुबह लें।"),
        "OMEPRAZOLE" to l("Take before food, usually in the morning.",
            "உணவுக்கு முன், பொதுவாக காலையில் எடுங்கள்.",
            "खाने से पहले, आमतौर पर सुबह लें।"),
    )

    // ingredient -> lang -> alcohol warning (only genuinely dangerous ones)
    private val ALCOHOL: Map<String, Map<String, String>> = mapOf(
        "PARACETAMOL" to l("Avoid alcohol — together it can harm the liver.",
            "மது வேண்டாம் — சேர்ந்தால் கல்லீரலைப் பாதிக்கலாம்.",
            "शराब से बचें — साथ में लीवर को नुकसान हो सकता है।"),
        "METFORMIN" to l("Avoid alcohol — risk of dangerous lactic acidosis.",
            "மது வேண்டாம் — ஆபத்தான லாக்டிக் அமிலத்தன்மை ஏற்படலாம்.",
            "शराब से बचें — खतरनाक लैक्टिक एसिडोसिस का खतरा।"),
        "ASPIRIN" to l("Avoid alcohol — higher risk of stomach bleeding.",
            "மது வேண்டாம் — வயிற்று இரத்தப்போக்கு அபாயம் அதிகரிக்கும்.",
            "शराब से बचें — पेट में रक्तस्राव का खतरा बढ़ता है।"),
        "IBUPROFEN" to l("Avoid alcohol — higher risk of stomach bleeding.",
            "மது வேண்டாம் — வயிற்று இரத்தப்போக்கு அபாயம் அதிகரிக்கும்.",
            "शराब से बचें — पेट में रक्तस्राव का खतरा बढ़ता है।"),
        "ACECLOFENAC" to l("Avoid alcohol — higher risk of stomach bleeding.",
            "மது வேண்டாம் — வயிற்று இரத்தப்போக்கு அபாயம் அதிகரிக்கும்.",
            "शराब से बचें — पेट में रक्तस्राव का खतरा बढ़ता है।"),
    )

    // ingredient -> lang -> pregnancy caution (only well-established "avoid" drugs)
    private val PREGNANCY: Map<String, Map<String, String>> = mapOf(
        "WARFARIN" to l("Avoid in pregnancy — can harm the baby. Ask your doctor.",
            "கர்ப்ப காலத்தில் வேண்டாம் — குழந்தையைப் பாதிக்கலாம். மருத்துவரிடம் கேளுங்கள்.",
            "गर्भावस्था में न लें — शिशु को नुकसान हो सकता है। डॉक्टर से पूछें।"),
        "ATORVASTATIN" to l("Avoid in pregnancy. Ask your doctor.",
            "கர்ப்ப காலத்தில் வேண்டாம். மருத்துவரிடம் கேளுங்கள்.",
            "गर्भावस्था में न लें। डॉक्टर से पूछें।"),
        "IBUPROFEN" to l("Avoid in late pregnancy. Ask your doctor.",
            "கர்ப்பத்தின் பிற்பகுதியில் வேண்டாம். மருத்துவரிடம் கேளுங்கள்.",
            "गर्भावस्था के अंत में न लें। डॉक्टर से पूछें।"),
        "ACECLOFENAC" to l("Avoid in late pregnancy. Ask your doctor.",
            "கர்ப்பத்தின் பிற்பகுதியில் வேண்டாம். மருத்துவரிடம் கேளுங்கள்.",
            "गर्भावस्था के अंत में न लें। डॉक्टर से पूछें।"),
    )

    /** All applicable advisories for a medicine's ingredients, in the given language. */
    fun advisories(ingredientIds: List<String>, lang: String): List<Advisory> {
        val out = ArrayList<Advisory>()
        fun add(icon: String, m: Map<String, Map<String, String>>) {
            val seen = HashSet<String>()
            ingredientIds.forEach { ing ->
                (m[ing]?.get(lang) ?: m[ing]?.get("en"))?.let { if (seen.add(it)) out.add(Advisory(icon, it)) }
            }
        }
        add("🍽️", FOOD)
        add("🍺", ALCOHOL)
        add("🤰", PREGNANCY)
        return out
    }

    private fun l(en: String, ta: String, hi: String) = mapOf("en" to en, "ta" to ta, "hi" to hi)
}
