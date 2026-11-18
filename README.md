# WordQuest AI v0.8.0

Android offline build repository. Project source is stored in split base64 ZIP files under `.wq-source/` because of the limited upload transport. GitHub Actions restores the source, generates English US/UK audio assets, runs checks, builds and uploads an Android APK artifact.

The default `offline` APK is a separately installable development variant; it does not upgrade a previously signed app. The app can require external configuration for AI/cloud features. Private API and signing keys must never be committed.
