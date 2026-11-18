# WordQuest AI — Ethan video review and v1.0.41 QA

**Source reviewed:** User-supplied 40.6-second Android landscape screen recording, 17 November 2026. 1728×1080; recorded TTS output spans about 4.5–36.4 s with several natural pauses (measured at about -32 dB silence threshold). This is screen-recorded audio, not access to the original TTS phonemes.

## Direct observations
- 0–4 s: Ethan's arms/torso show a brief more noticeable gesture.
- ~8–36 s: full-screen 3D Ethan speaks and the UI labels him speaking. Arms often hang beside the body, offering limited explanatory gesture.
- ~24–32 s: head and upper torso lean markedly sideways, more than comfortable teacher posture.
- Mouth movement is visually subtle at medium camera distance; phonetically accurate synchronization cannot be established from this video alone.
- ~38 s: stage returns to idle with mouth closed, matching the end of audible speech. No startup crash is visible in the sample.

## QA changes from this evidence
- **Bone pose:** tune original Talk rotations of head/neck/spine toward their own actual Idle rotations, preserving the same animation accessors and skinned human 3D geometry. Restore part of the original arm swing lost by over-attenuation in 1.0.40.
- **Mouth clarity:** undo some of 1.0.40's over-aggressive mouth weight reduction while keeping the jaw below the original extreme.
- **Facial stability:** one-shot morph clips only restart on an actual new word cue or facial expression change, not merely because the previous one-shot ended.
- **No fake sync:** use Android onRangeStart text spans, not invented sound energy or hidden phonemes; genuine phoneme-level accuracy would require a different audio/viseme backend.

## Manual acceptance before signing a new release
- [ ] 10-second English reply: head no longer tilts dramatically as seen in the 24–32 s sample
- [ ] Arms gesture in a small controlled range, no fixed dangling posture or repeated high-speed flailing
- [ ] TTS mouth opens on word-range cues and returns neutral at a pause and on completion
- [ ] Eyelids blink once naturally; no flickering/restarting face clips
- [ ] New conversation, Sesi kes, US/UK voice, Gemini/OpenAI, 3D open/close all work
- [ ] Same package name and signer as 1.0.40; old conversations, keys, Emma/Sophia archive and other game modes preserved

Only a test branch exists until physical Android verification. Earlier published 1.0.40 continues working. Do not uninstall, reset the app, or request personal API keys.
