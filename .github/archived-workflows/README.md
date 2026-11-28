# CI archive and release efficiency

The 22 historical workflows in this directory are byte-identical copies of previously active workflows, retained for rollback and investigation. They no longer run automatically when this maintenance commit is integrated. To restore one, copy it back to `.github/workflows/` and check its triggers and version targets first.

- Keep the current verified release package workflow, Arena match regression, server/rules, learning content and pronunciation checks.
- Full assembled-source audit is opt-in (`full-audit` PR label or supported manual dispatch), since the release package workflow already compiles, lints, signs and tests the APK.
- Do not delete published releases, delta bases, source overlays, assets, signing configuration or caches based on age alone.
- The archive paths are excluded from the stable assembly cache fingerprint in both current assembly workflows; their move alone must not invalidate source caches.
- Integrate only after the active v1.0.153 release package run has ended. Future releases need a new version number/base; never republish the same version with changed bytes.
