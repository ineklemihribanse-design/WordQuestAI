# Arena v150 server (staging only until release gates pass)

Build from repository root: docker build -f tools/arena_v150_server/Dockerfile -t wordquest-arena-v150 .

Runtime requires FIREBASE_PROJECT_ID, FIREBASE_DATABASE_URL, ARENA_ROOM_SECRET and ARENA_SCHEDULER_SECRET (distinct, minimum 32 characters), and ARENA_BANK_FILE (mounted curated JSON manifest). The runtime uses Application Default Credentials; use a dedicated service identity with necessary Firebase Auth and RTDB access. Do not commit credentials or answer banks. Do not enable the Android origin before staging validation.

Manifest schema 150: quizzes array with id, prompt, five unique options, zero-based correct, level and optional yds/topic/wordType. boards array with id and words (id, uppercase answer, clue, level, optional filter fields, contiguous row,col cells). Include actual curated content matching supported filters and counts; the service rejects unavailable combinations rather than fabricating content.

Expose HTTPS through Cloud Run. Configure a trusted scheduled POST /internal/sweep with x-arena-scheduler-secret from secret storage. Active clients also sweep at heartbeat. A once-per-minute scheduler can delay final persistence when all clients disappear, although the reducer applies expirations at their deadlines. Do not enable production without measuring this and testing actual devices. Apply private RTDB rules and index only after diffing against live rules. Never overwrite unrelated rules blindly.

No live deployment is performed by this commit. Required before release: verified staging Firebase project and runtime credentials, real curated bank, successful deploy and health check, two/four-device smoke test, cost and abuse controls, APK signature/data preservation and release approval.

For the shipped Arena dictionary, set ARENA_BANK_FILE to the verified arena-vocabulary-v1.json artifact from the full-source audit. Server generation reuses the existing connected crossword engine and source translations. It rejects any selected level/filter that lacks explicit source metadata. YDS is available only if records explicitly provide a valid yds field as well as a CEFR level; no inferred difficulty. The actual audit report lists available coverage. The curated manifest remains an alternative.
