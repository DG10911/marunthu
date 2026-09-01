package com.marunthu.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Offline spoken output via Android's built-in TextToSpeech engine.
 *
 * IMPORTANT (demo prerequisite): the Tamil / Hindi / English voice data must be installed on
 * the device BEFORE going into airplane mode (Settings > Accessibility > Text-to-speech >
 * Install voice data). Once installed, synthesis is fully offline. [availabilityFor] lets the
 * UI gracefully fall back to large on-screen text when a language's voice data is missing.
 */
class TtsService(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }

    /** LANG_AVAILABLE / LANG_MISSING_DATA / LANG_NOT_SUPPORTED — used for graceful fallback. */
    fun availabilityFor(bcp47: String): Int =
        if (!ready) TextToSpeech.LANG_NOT_SUPPORTED
        else tts.isLanguageAvailable(Locale.forLanguageTag(bcp47))

    fun isSpeakable(bcp47: String): Boolean =
        availabilityFor(bcp47) >= TextToSpeech.LANG_AVAILABLE

    fun speak(text: String, bcp47: String) {
        if (!ready) return
        tts.language = Locale.forLanguageTag(bcp47)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "marunthu")
    }

    fun stop() { if (ready) tts.stop() }
    fun shutdown() { tts.stop(); tts.shutdown() }
}
