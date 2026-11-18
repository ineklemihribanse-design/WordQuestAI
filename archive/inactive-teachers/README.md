# WordQuest AI — Passive Teacher Archive (Emma, Sophia)

**Product decision (17 November 2026):** Ethan is the **only active 3D AI conversation teacher**. Emma and Sophia are **temporarily inactive, not deleted**. Do not re-enable or publish them without explicit approval.

## Preservation and location

The repository retains all source generation, provenance, license and repair scripts for all three real-human teachers: `tools/v126`, `tools/v127`, `tools/v1271`, `tools/v130` through `tools/v133`. Before the next APK is assembled, the v1.0.34 CI step `tools/v134/archive_inactive_teachers_134.py`:

- moves *all* staged `emma*.glb` and `sophia*.glb` plus relevant staged source assets to `source/archived-teachers/emma/` and `source/archived-teachers/sophia/`;
- saves original full avatar provenance and lip metadata to `source/archived-teachers/` and writes an `ARCHIVE_MANIFEST.json` containing SHA-256 digests;
- packages this directory as a separate `Inactive-Human-Teacher-Archive` GitHub Actions artifact (not bundled into the Android APK);
- leaves Ethan's proven working `ethan-mobile.glb` and Ethan's UI/AI/voice unchanged;
- filters active APK provenance so it only advertises Ethan.

All model source recipes remain in version control and can reproduce Emma and Sophia after artifact expiry. **Do not delete those original scripts, generated source manifests or archive artifacts when restoring them.** These inactive teacher assets must not cause Filament initialization in current live chat.

## Restore later

1. Retrieve the most recent `Inactive-Human-Teacher-Archive` artifact from CI or regenerate exact source GLBs with retained historical scripts.
2. Validate their hashes and MIT Rocketbox human-model license notices.
3. Move selected GLB files back to `source/app/src/main/assets/avatars/` only on a dedicated QA branch.
4. Reinstate teacher UI options only after Android native-GLB stability is demonstrated and explicit user approval is obtained.

**Data safety:** existing old Emma/Sophia local conversations/notes are not erased or migrated into Ethan's history. Temporarily hiding their selection does not delete stored profile data.

**Scope:** This is an asset/archive and active UI change; it does not replace the real human Ethan with a 2D, anime or geometric stand-in.
