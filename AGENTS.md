# WordQuest AI coding-agent instructions

This is the Android Kotlin/Jetpack Compose project **WordQuest AI**. Treat the user's Turkish requirements as authoritative.

## Setup and repository layout
- The existing compilable Android Studio/Gradle project is archived as `wq-noaudio.zip` in the repository root.
- For Codex/local development, first run `bash scripts/prepare-codex-project.sh`. This extracts the project under `app-project/`. Open that folder in Android Studio.
- The archive intentionally does not include the large generated audio clips. The Android build CI regenerates them from `tools/build_offline_voices.py`; do not claim that these robotic eSpeak recordings are natural voices.
- The existing GitHub Actions workflow `.github/workflows/build-apk.yml` builds the baseline v0.8.0 offline APK from the archive. If source files are modified inside `app-project/`, change CI to build those updated files; NEVER silently ship the old archived source.
- Do not commit credentials, signing keys, API keys, or privately supplied user files.

## Product requirements for next version
- The user wants **no additional payments**: no paid API, speech subscription, paid service, or surprise cloud costs.
- Replace robotic eSpeak pronunciation with genuinely more natural US/UK English speech. Prefer open-source local neural TTS models, but first check redistribution rights, size, performance and quality. Generate and listen to a small 4-voice sample set (US female/male, UK female/male), and verify word-to-audio mapping before generating all assets.
- Preserve all 2,203 expressions and both accent mappings; retain offline playback, auto-play on correct puzzle, replay on touch, speed/volume/auto-play settings.
- Preserve distinct US/UK IPA, optional approximate Turkish pronunciation, IPA visibility preference; do not fabricate IPA where uncertain or conflict with audio.
- Preserve all older puzzles, competition system, separate learner profiles/data, searchable persistent AI conversation history, student memory, personalized lessons, spaced repetition, adjustable font size.
- Android Studio must be able to open and build the complete project. Test Kotlin compilation, automated tests, manifest, assets, app launch (emulator when available), and APK signature. Deliver APK whenever actually built and tested. Make clear if any cloud AI feature still requires an external API key or service.
- The v0.8.0 offline flavor is independently installable: do not claim it upgrades an older installed app without matching app ID and signing certificate.
- Do not start bulk model synthesis or cloud jobs that might incur fees without confirming costs are zero and the user has approved any unavoidable paid resource.

## Validation commands (agent-executed, not user instructions)
- `cd app-project && python3 tools/verify_audio_ipa.py` (after audio is present)
- `cd app-project && ./gradlew :app:testOfflineUnitTest :app:assembleOffline`
- `apksigner verify --verbose <output-apk>`

Respond in Turkish when reporting to the user. Be transparent about untested functionality.
