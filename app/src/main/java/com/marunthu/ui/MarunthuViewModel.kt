package com.marunthu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marunthu.core.expiry.ExpiryInfo
import com.marunthu.core.expiry.ExpiryParser
import com.marunthu.core.medicine.MedicineMatcher
import com.marunthu.core.medicine.Substitute
import java.util.Calendar
import com.marunthu.core.medicine.SubstituteFinder
import com.marunthu.core.model.Medicine
import com.marunthu.core.model.MedicineCandidate
import com.marunthu.core.model.SafetyStatus
import com.marunthu.core.model.StructuredResult
import com.marunthu.core.safety.SafetyEngine
import com.marunthu.data.CatalogLoader
import com.marunthu.data.DemoCatalog
import com.marunthu.data.MyMedsRepository
import kotlinx.coroutines.Dispatchers
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
    val catalogSize: Int = 0,            // number of medicines loaded (shown on home)
    val expiryInfo: ExpiryInfo? = null,  // EXP date read from the strip
    val expiryToxic: Boolean = false,    // active ingredient turns harmful once expired
)

/**
 * Orchestrates the offline pipeline: OCR text -> match -> safety engine -> localized message
 * -> TTS. All synchronous, all on-device. No coroutine/network needed once OCR returns text.
 */
class MarunthuViewModel(app: Application) : AndroidViewModel(app) {

    // Start with the small demo catalog (instant); the full 60k catalog loads in the
    // background and swaps in (see loadFullCatalog).
    @Volatile private var matcher = MedicineMatcher(DemoCatalog.medicines)
    private val engine = SafetyEngine(DemoCatalog.rules)
    @Volatile private var substitutes = SubstituteFinder(DemoCatalog.medicines)
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
    init {
        refreshMyMeds()
        loadFullCatalog()
    }

    /** Load the full 60k offline catalog off the main thread, then swap in the matcher. */
    private fun loadFullCatalog() {
        viewModelScope.launch(Dispatchers.Default) {
            val big = CatalogLoader.load(getApplication())
            if (big.isNotEmpty()) {
                val seen = HashSet<String>()
                val combined = (DemoCatalog.medicines + big).filter { seen.add(it.canonicalId) }
                matcher = MedicineMatcher(combined)
                substitutes = SubstituteFinder(combined)
                _state.value = _state.value.copy(catalogSize = combined.size)
            } else {
                _state.value = _state.value.copy(catalogSize = DemoCatalog.medicines.size)
            }
        }
    }

    fun setLanguage(code: String) {
        _state.value = _state.value.copy(language = code)
        recomputeMessage()
        refreshTtsAvailability()
    }

    /** Called with raw ML Kit OCR text after each capture. Adds the best candidate. */
    fun onOcrText(text: String) {
        val best = matcher.match(text).firstOrNull()
        // Refuse to guess: a low-confidence match on messy OCR would be a dangerous false
        // positive. Ask the user to aim at the brand name or the ingredient (salt) list.
        if (best == null || best.confidence < 0.70) {
            _state.value = _state.value.copy(
                lastOcrText = text,
                hint = "Couldn't identify this medicine clearly. Point at the brand name or the " +
                    "ingredient list, and hold steady in good light.",
            )
            return
        }
        val scanned = (_state.value.scanned + best).takeLast(2)
        // Read the EXP date off the same strip (expiry intelligence).
        val cal = Calendar.getInstance()
        val exp = ExpiryParser.parse(text, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val toxic = exp != null && ExpiryParser.becomesToxicWhenExpired(best.medicine.ingredientIds)
        _state.value = _state.value.copy(
            lastOcrText = text, scanned = scanned, hint = null,
            expiryInfo = exp, expiryToxic = toxic,
        )
        evaluate()
    }

    /** For the failure-resistant demo: inject a known candidate without the camera. */
    fun addSampleById(canonicalId: String) {
        val m = DemoCatalog.medicines.firstOrNull { it.canonicalId == canonicalId } ?: return
        val scanned = (_state.value.scanned + MedicineCandidate(m, 0.97, m.brandName)).takeLast(2)
        _state.value = _state.value.copy(scanned = scanned, expiryInfo = null, expiryToxic = false)
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
            expiryInfo = null, expiryToxic = false,
        )
    }

    override fun onCleared() { tts.shutdown() }
}
