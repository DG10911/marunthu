package com.marunthu.data

/**
 * Curated, prototype "what it's used for" lines per active ingredient, in Tamil / Hindi /
 * English. Kept deliberately short + conservative (no dosing advice). Language-independent
 * keys (ingredient ids) resolved to localized text — same pattern as the rest of the app.
 */
object MedicineInfo {

    private val uses: Map<String, Map<String, String>> = mapOf(
        "PARACETAMOL" to mapOf(
            "en" to "Used for fever and mild pain.",
            "ta" to "காய்ச்சல் மற்றும் லேசான வலிக்கு.",
            "hi" to "बुखार और हल्के दर्द के लिए।"),
        "IBUPROFEN" to mapOf(
            "en" to "Used for pain, swelling and fever.",
            "ta" to "வலி, வீக்கம் மற்றும் காய்ச்சலுக்கு.",
            "hi" to "दर्द, सूजन और बुखार के लिए।"),
        "ACECLOFENAC" to mapOf(
            "en" to "Used for pain and joint inflammation.",
            "ta" to "வலி மற்றும் மூட்டு வீக்கத்திற்கு.",
            "hi" to "दर्द और जोड़ों की सूजन के लिए।"),
        "METFORMIN" to mapOf(
            "en" to "Controls blood sugar in type-2 diabetes.",
            "ta" to "நீரிழிவில் இரத்த சர்க்கரையை கட்டுப்படுத்த.",
            "hi" to "टाइप-2 डायबिटीज़ में शुगर नियंत्रण के लिए।"),
        "AMOXICILLIN" to mapOf(
            "en" to "An antibiotic for bacterial infections.",
            "ta" to "பாக்டீரியா தொற்றுக்கான ஆண்டிபயாட்டிக்.",
            "hi" to "बैक्टीरियल संक्रमण के लिए एंटीबायोटिक।"),
        "AZITHROMYCIN" to mapOf(
            "en" to "An antibiotic for bacterial infections.",
            "ta" to "பாக்டீரியா தொற்றுக்கான ஆண்டிபயாட்டிக்.",
            "hi" to "बैक्टीरियल संक्रमण के लिए एंटीबायोटिक।"),
        "AMLODIPINE" to mapOf(
            "en" to "Lowers high blood pressure.",
            "ta" to "உயர் ரத்த அழுத்தத்தை குறைக்க.",
            "hi" to "हाई ब्लड प्रेशर कम करने के लिए।"),
        "TELMISARTAN" to mapOf(
            "en" to "Lowers high blood pressure.",
            "ta" to "உயர் ரத்த அழுத்தத்தை குறைக்க.",
            "hi" to "हाई ब्लड प्रेशर कम करने के लिए।"),
        "ATORVASTATIN" to mapOf(
            "en" to "Lowers high cholesterol.",
            "ta" to "அதிக கொழுப்பை (கொலஸ்ட்ரால்) குறைக்க.",
            "hi" to "हाई कोलेस्ट्रॉल कम करने के लिए।"),
        "ASPIRIN" to mapOf(
            "en" to "Low dose thins blood and prevents clots.",
            "ta" to "இரத்தத்தை நீர்த்து உறைவதை தடுக்க.",
            "hi" to "खून पतला करने और थक्के रोकने के लिए।"),
        "WARFARIN" to mapOf(
            "en" to "A blood thinner that prevents clots.",
            "ta" to "இரத்த உறைவைத் தடுக்கும் மருந்து.",
            "hi" to "खून का थक्का रोकने वाली दवा।"),
        "CETIRIZINE" to mapOf(
            "en" to "Relieves allergy and sneezing.",
            "ta" to "ஒவ்வாமை மற்றும் தும்மலை குறைக்க.",
            "hi" to "एलर्जी और छींक से राहत के लिए।"),
        "PANTOPRAZOLE" to mapOf(
            "en" to "Reduces stomach acid and acidity.",
            "ta" to "வயிற்று அமிலம் மற்றும் புளிப்பை குறைக்க.",
            "hi" to "पेट का एसिड और एसिडिटी कम करने के लिए।"),
        "OMEPRAZOLE" to mapOf(
            "en" to "Reduces stomach acid and acidity.",
            "ta" to "வயிற்று அமிலம் மற்றும் புளிப்பை குறைக்க.",
            "hi" to "पेट का एसिड और एसिडिटी कम करने के लिए।"),
        "DOMPERIDONE" to mapOf(
            "en" to "Relieves nausea and vomiting.",
            "ta" to "குமட்டல் மற்றும் வாந்தியை குறைக்க.",
            "hi" to "मतली और उल्टी से राहत के लिए।"),
        "LEVOTHYROXINE" to mapOf(
            "en" to "Treats low thyroid (hypothyroidism).",
            "ta" to "தைராய்டு குறைபாட்டிற்கு.",
            "hi" to "थायरॉइड की कमी के इलाज के लिए।"),
    )

    /** One combined "used for" line for a medicine's ingredients, or null if unknown. */
    fun usesFor(ingredientIds: List<String>, lang: String): String? {
        val lines = ingredientIds.mapNotNull { uses[it]?.get(lang) ?: uses[it]?.get("en") }
        return lines.distinct().takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}
