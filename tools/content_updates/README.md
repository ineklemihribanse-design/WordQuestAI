# WordQuest signed content updater — development stage
First phase: existing WebP Arena and New Game artwork only. No executable downloads, APK installation, or modification of Samsung security settings. The app verifies the manifest using its installed signing certificate, verifies each SHA-256 and bounded image dimensions, stages a complete snapshot inside noBackupFilesDir, and commits a pointer only after validation. The running session does not change artwork; the next cold start activates it. Old snapshots are not automatically pruned in this prototype.

Apply after assembling the exact v1.0.123 source in the established CI pipeline:
`python3 tools/content_updates/apply_content.py`.
Then integrate into a **new** APK build/version, run all unit tests and real-device tests, and verify the original signer. Do not publish until tested. The current APK and release channel are untouched.

The separate content-channel branch is intentionally not published yet. For the first test, build a cumulative snapshot with make_content.py and the release signing key in a secure CI environment (never commit keys). Publish content-addressed objects and index.json together; index must be signed by the installed APK signer. Core code must match the installed APK exactly. Versions and sequence increase monotonically. Keep the APK release channel separate.

Remaining: CI integration and signed content publishing automation, content version on all relevant screens, reliable rollback and retention policy, device tests including Samsung Auto Blocker enabled, and actual adapters for Lena, vocab, sound, and declarative UI. No claim that arbitrary Kotlin/Compose code can be updated without Android package installation.
