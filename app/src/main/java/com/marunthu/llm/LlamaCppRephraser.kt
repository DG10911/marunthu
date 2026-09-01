package com.marunthu.llm

import com.marunthu.core.model.StructuredResult
import com.marunthu.lang.LocalizedSafetyMessage
import java.io.File

/**
 * On-device Sarvam-1 rephraser via a llama.cpp GGUF runtime. INTEGRATION STUB.
 *
 * How to finish (done on the DGX + one JNI wire-up — see .context/DGX_RUNBOOK.md):
 *  1. On the DGX: convert sarvamai/sarvam-1 -> GGUF, quantize to int4 (~1.2 GB).
 *  2. Ship the .gguf in app assets (or download-once to filesDir on first launch, with a
 *     Wi-Fi prompt — never during the offline demo).
 *  3. Add a llama.cpp Android build (libllama.so via CMake/NDK) and a tiny JNI bridge with
 *     `nativeInfer(modelPtr, prompt): String`; call it from [rephrase] on Dispatchers.Default
 *     with a short timeout.
 *
 * Until the native lib is wired, [isAvailable] is false and [rephrase] returns null, so the
 * app cleanly uses the deterministic L2 templates. This keeps L3 strictly off the critical
 * path — the base app compiles and demos with no model present.
 */
class LlamaCppRephraser(private val modelPath: String) : LlmRephraser {

    private val modelPresent: Boolean = runCatching { File(modelPath).exists() }.getOrDefault(false)

    // Flip to true only once the JNI runtime below is implemented and the model loads.
    override val isAvailable: Boolean = false

    override suspend fun rephrase(
        structured: StructuredResult,
        base: LocalizedSafetyMessage,
        langCode: String,
    ): String? {
        if (!isAvailable || !modelPresent) return null
        val prompt = PromptBuilder.build(base, langCode)
        return runCatching { nativeInfer(prompt) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    /** JNI entry point — implement in native/llama_jni.cpp. Placeholder throws until wired. */
    private external fun nativeInfer(prompt: String): String

    companion object {
        // System.loadLibrary("marunthu_llm") — enable once the .so is built.
        const val NATIVE_LIB = "marunthu_llm"
        const val DEFAULT_MODEL_FILE = "sarvam-1-q4_k_m.gguf"
    }
}
