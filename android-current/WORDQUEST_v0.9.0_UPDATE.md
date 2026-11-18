# WordQuest AI v0.9.0 — integrated development version

- The **same crossword does not automatically advance to the next word** after a correct answer. The user selects another clue/word using grid, clue list, or explicit Next key.
- Correctly typed words play a short, gentle acknowledgement effect **first**; optional word pronunciation plays **after a 410 ms delay**, only if the corresponding user settings allow it. The options are independent.
- Preserves the existing voice system, the chosen US/UK accent, the UK-compatibility change, offline fallback, optional IPA, text scaling, saved boards and learner notes.
- v0.9.0 personal practice hub: dictation, scrambled sentence, grammar correction, story completion, speech-to-text comparison and crosswords from personally mistaken words (at least six unique answers required for a board).
- Daily plans: 15 / 30 / 60 minutes, foreground time and unique exercise completion saved **per active profile** and per day.
- AI teacher: persistent searchable threads, personal learner memory notes, lesson/practice/grammar/correction modes and Turkish explanations are retained from the earlier version; online replies require a configured provider/API key and connectivity.
- Multiplayer arena: original two-device room code, contest modes, AI judge options and score histories are retained; functional online rooms need an explicitly configured/deployed backend and credentials. Never claim this is available by default.
- **Update compatibility**: v0.9.0 keeps v0.8.2 package ID `com.kelimebulmacasi.a1.practice.offline082`. CI builds an unsigned APK; sign it with the *existing v0.8.2 PKCS12 key locally*, and verify identical signer certificate before shipping. No key material or passwords are committed to the repository.
- Neither simulator nor real Android-device user flows have been verified unless a separate Android instrumentation run is available. Do not claim actual device success on build-only evidence.
