# Marunthu 💊

**Medicine safety. In your language. Without the internet.**

An offline-first, multilingual medication-safety assistant for Android. Point the camera at a
medicine strip → it reads the (Latin) drug name on-device → checks a local safety database for
duplicate active ingredients and interactions → **explains and speaks the result in Tamil / Hindi /
English** → **works fully in airplane mode**. No image, OCR text, or health data ever leaves the phone.

Built for iQOO City Battles 2026 (Chennai, HealthTech). Design rationale + research in
`../.context/MARUNTHU_BUILD_SPEC.md`.

## Architecture (offline, layered, LLM is never the medical authority)
```
CameraX → ML Kit Latin OCR → TextNormalizer → MedicineMatcher (fuzzy, confidence)
        → canonical Medicine → SafetyEngine (deterministic rules)
        → StructuredResult (language-independent)
        → LanguageEngine templates (L2)  [+ optional Sarvam-1 rephrase = L3]
        → Android TextToSpeech (offline)
```
- **Medical core is pure Kotlin & language-independent** (`core/`, `data/`) — unit tested.
- **Language is a thin presentation layer** (`lang/`) — add a language without touching medicine logic.
- **No `INTERNET` permission is declared** (see `AndroidManifest.xml`) — structural privacy proof.

## Build & run
1. Open the `marunthu/` folder in **Android Studio** (Ladybug or newer). It will generate the
   Gradle wrapper JAR on first sync. (CLI alternative: `gradle wrapper` then `./gradlew :app:assembleDebug`.)
2. Run unit tests (no device needed — this is the testable medical core):
   ```
   ./gradlew :app:testDebugUnitTest
   ```
3. Install on the iQOO 15: `./gradlew :app:installDebug`.
4. **Before the demo**, install offline TTS voices on the device:
   Settings → Accessibility → Text-to-speech → Google TTS → *Install voice data* → Tamil, Hindi, English.

## Demo flow (the magic moment)
1. Toggle **✈️ airplane mode ON**.
2. Scan **Dolo 650**, then **Combiflam** (or use the on-screen *Sample* buttons as a backup).
3. Marunthu detects both contain **Paracetamol** → speaks the warning **in Tamil**.
4. Tap **हिन्दी**, then **English** → same verdict, different language. *"The medicine is universal,
   the intelligence is universal — only the conversation changes."*

## Status — what's done vs next
**Done (this scaffold):**
- Pure-Kotlin medical core: domain model, `TextNormalizer`, `MedicineMatcher` (fuzzy + confidence),
  `SafetyEngine` (duplicate / strength / interaction / uncertainty), curated `DemoCatalog`.
- `LanguageEngine` with real Tamil/Hindi/English templates (L2).
- Android: ML Kit OCR, Android TTS wrapper (with availability fallback), CameraX scan screen,
  Home/Scan/Result Compose UI, ViewModel wiring the full pipeline.
- Unit tests for the safety engine + matcher.

**Next (in priority order):**
1. Verify build in Android Studio; run on device; tune capture (guide box, blur/glare check).
2. Integrate **Sarvam-1 2B (GGUF int4)** via llama.cpp as the optional L3 explanation rephraser
   (behind the template fallback — never on the critical path). Prep the model on the DGX.
3. Expand `DemoCatalog` from the Indian-Medicine-Dataset CSV; optionally move to Room + FTS.
4. Optional voice input (whisper.cpp tiny, Tamil) — keep it strictly optional.
5. Premium UI pass; record the airplane-mode backup video for the submission.

## Safety
Prototype dataset only. Marunthu does **not** diagnose, prescribe, or guarantee interaction
coverage. Every screen shows: *always confirm medication changes with a qualified professional.*
