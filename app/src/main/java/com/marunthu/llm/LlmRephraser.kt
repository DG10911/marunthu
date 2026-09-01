package com.marunthu.llm

import com.marunthu.core.model.StructuredResult
import com.marunthu.lang.LocalizedSafetyMessage

/**
 * LEVEL 3 of the intelligence stack: an OPTIONAL on-device LLM that rephrases the
 * deterministic L2 template into warmer, more natural language for the chosen language.
 *
 * Hard rules (see MARUNTHU_BUILD_SPEC §6):
 *  - The LLM NEVER decides the medical verdict. It only receives the already-decided
 *    [StructuredResult] + the deterministic [LocalizedSafetyMessage] and rewords it.
 *  - It must never invent drugs, doses, or interactions. The prompt forbids it and the
 *    caller keeps the deterministic template if anything looks off.
 *  - It is strictly optional: [rephrase] returns null whenever the model is unavailable,
 *    slow, or errors, and the app falls back to the L2 template. The whole demo works
 *    with zero LLM.
 */
interface LlmRephraser {
    /** @return improved text, or null to keep the deterministic template. */
    suspend fun rephrase(
        structured: StructuredResult,
        base: LocalizedSafetyMessage,
        langCode: String,
    ): String?

    /** True if a model is actually loaded and ready. Drives UI hints only. */
    val isAvailable: Boolean
}

/** Default L3: no LLM. Always falls back to the L2 template. Ships in the base app. */
object TemplateOnlyRephraser : LlmRephraser {
    override val isAvailable = false
    override suspend fun rephrase(
        structured: StructuredResult,
        base: LocalizedSafetyMessage,
        langCode: String,
    ): String? = null
}

/**
 * Builds the constrained prompt handed to Sarvam-1 (or any local GGUF model). Kept pure
 * so it is unit-testable and reused by the on-device runtime.
 */
object PromptBuilder {
    fun build(base: LocalizedSafetyMessage, langCode: String): String = """
        You are a careful medical-safety assistant. Rewrite the SAFETY MESSAGE below into
        one or two short, simple, respectful sentences in language code "$langCode", for a
        patient who may not read well. Do NOT add any medicine names, doses, or facts that
        are not already in the message. Do NOT diagnose. Keep the same meaning. If unsure,
        repeat the message as-is.

        SAFETY MESSAGE:
        ${base.body}

        REWRITTEN:
    """.trimIndent()
}
