package com.marunthu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marunthu.core.medicine.MedicineMatcher
import com.marunthu.core.medicine.Substitute
import com.marunthu.core.medicine.SubstituteFinder
import com.marunthu.core.model.Medicine
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.model.StructuredResult
import com.marunthu.core.safety.SafetyEngine
import com.marunthu.data.DemoCatalog
import com.marunthu.data.MyMedsRepository
import com.marunthu.lang.LanguageEngine
import com.marunthu.lang.LocalizedSafetyMessage
import com.marunthu.llm.LlmRephraser
import com.marunthu.llm.TemplateOnlyRephraser
import com.marunthu.tts.TtsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val language: String = "ta",
    val scanned: List<MedicineCandidate> = emptyList(),
    val lastOcrText: String = "",
    val result: StructuredResult? = null,
    val message: LocalizedSafetyMessage? = null,
    val ttsAvailable: Boolean = true,
    val hint: String? = null,
    val substitute: Substitute? = null,
    val myMeds: List<Medicine> = emptyList(),
    val profileWarning: String? = null,  // scanned med clashes with a saved My-Med
)

/**
 * Orchestrates the offline pipeline: OCR text -> match -> safety engine -> localized message
 * -> TTS. All synchronous, all on-device. No coroutine/network needed once OCR returns text.
 */
class MarunthuViewModel(app: Application) : AndroidViewModel(app) {

    private val matcher = MedicineMatcher(DemoCatalog.medicines)
    private val engine = SafetyEngine(DemoCatalog.rules)
    private val substitutes = SubstituteFinder(DemoCatalog.medicines)
    private val myMedsRepo by lazy { MyMedsRepository(getApplication()) }
    private val tts by lazy { TtsService(getApplication()) }

    private fun refreshMyMeds() {
        val ids = myMedsRepo.ids()
        val meds = DemoCatalog.medicines.filter { it.canonicalId in ids }
        _state.value = _state.value.copy(myMeds = meds)
    }

    /** Add the most recently scanned medicine to the user's saved regimen. */
    fun addPrimaryToMyMeds() {
        val id = _state.value.scanned.lastOrNull()?.canonicalId ?: return
        myMedsRepo.add(id)
        refreshMyMeds()
    }

    fun removeFromMyMeds(canonicalId: String) {
        myMedsRepo.remove(canonicalId)
        refreshMyMeds()
    }

    /** Proactive check: does the scanned medicine clash with anything already saved? */
    private fun proactiveCheck(primary: MedicineCandidate?): String? {
        if (primary == null) return null
        val saved = _state.value.myMeds.filter { it.canonicalId != primary.canonicalId }
        if (saved.isEmpty()) return null
        val savedCands = saved.map { MedicineCandidate(it, 1.0, it.brandName) }
        val warning = engine.evaluate(listOf(primary) + savedCands).firstOrNull { r ->
            r.status == SafetyStatus.WARNING &&
                (r.medicineA?.canonicalId == primary.canonicalId ||
                 r.medicineB?.canonicalId == primary.canonicalId)
        } ?: return null
        return LanguageEngine.explain(warning, _state.value.language).body
    }

    // L3 (optional). Default = templates only. Swap to LlamaCppRephraser(modelPath) once the
    // Sarvam-1 GGUF + native lib are bundled — the app behaves identically until then.
    private val rephraser: LlmRephraser = TemplateOnlyRephraser

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Runs AFTER _state is initialized (Kotlin executes init blocks in declaration order).
    init { refreshMyMeds() }

    fun setLanguage(code: String) {
        _state.value = _state.value.copy(language = code)
        recomputeMessage()
        refreshTtsAvailability()
    }

    /** Called with raw ML Kit OCR text after each capture. Adds the best candidate. */
    fun onOcrText(text: String) {
        val best = matcher.match(text).firstOrNull()
        if (best == null) {
            _state.value = _state.value.copy(
                lastOcrText = text,
                hint = "Couldn't read the strip. Hold steady, add light, or use a Sample below.",
            )
            return
        }
        val scanned = (_state.value.scanned + best).takeLast(2)
        _state.value = _state.value.copy(lastOcrText = text, scanned = scanned, hint = null)
        evaluate()
    }

    /** For the failure-resistant demo: inject a known candidate without the camera. */
    fun addSampleById(canonicalId: String) {
        val m = DemoCatalog.medicines.firstOrNull { it.canonicalId == canonicalId } ?: return
        val scanned = (_state.value.scanned + MedicineCandidate(m, 0.97, m.brandName)).takeLast(2)
        _state.value = _state.value.copy(scanned = scanned)
        evaluate()
    }

    private fun evaluate() {
        val result = engine.evaluate(_state.value.scanned).firstOrNull()
        // Cheapest same-composition alternative for the most recently scanned medicine.
        val primaryCand = _state.value.scanned.lastOrNull()
        val substitute = primaryCand?.medicine?.let { substitutes.cheaperAlternatives(it).firstOrNull() }
        val profileWarning = proactiveCheck(primaryCand)
        _state.value = _state.value.copy(
            result = result, substitute = substitute, profileWarning = profileWarning,
        )
        recomputeMessage()
    }

    private fun recomputeMessage() {
        val r = _state.value.result ?: return
        val base = LanguageEngine.explain(r, _state.value.language)
        _state.value = _state.value.copy(message = base)
        // Optional L3 enhancement — runs async, never blocks the UI, always falls back to
        // the deterministic template if the model is absent, slow, or errors.
        if (rephraser.isAvailable) {
            val lang = _state.value.language
            viewModelScope.launch {
                val improved = runCatching { rephraser.rephrase(r, base, lang) }.getOrNull()
                if (improved != null && _state.value.result === r && _state.value.language == lang) {
                    _state.value = _state.value.copy(
                        message = base.copy(body = improved, spokenText = improved),
                    )
                }
            }
        }
    }

    fun speakCurrent() {
        val msg = _state.value.message ?: return
        val bcp47 = LanguageEngine.profile(_state.value.language).ttsBcp47
        if (tts.isSpeakable(bcp47)) tts.speak(msg.spokenText, bcp47)
    }

    private fun refreshTtsAvailability() {
        val bcp47 = LanguageEngine.profile(_state.value.language).ttsBcp47
        _state.value = _state.value.copy(ttsAvailable = tts.isSpeakable(bcp47))
    }

    fun reset() {
        _state.value = _state.value.copy(
            scanned = emptyList(), result = null, message = null,
            lastOcrText = "", hint = null, substitute = null, profileWarning = null,
        )
    }

    override fun onCleared() { tts.shutdown() }
}
