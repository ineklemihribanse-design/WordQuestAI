# Lena asset manifest — WordQuest AI

Lena is an independent optional teacher; do not modify Alex or the home screen.

## Asset intended for repository
- Source: `Lena.obj`
- Latest rigged export: `Lena_Prism3D_AnimationPack_v6.glb`
- Target asset path: `app/src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb` (after project extraction)
- Source of truth: latest GLB, not older experimental versions
- GLB inspection: 16 animation clips; skin and skeletal animation data present
- On-device import, deformation and Android runtime are **not yet validated**

## Animation states and fallback mapping
| App state | GLB clip |
| --- | --- |
| idle | 01_Idle_Natural_Breathing |
| greet | 02_Greeting_Gentle_Wave |
| speak | 03_Talking_Hand_Gestures |
| listen | 04_Listening_Nod |
| thinking | 13_Thinking_Softly |
| smile | 16_Relaxed_Smile |
| blink | 06_Face_Soft_Blink |

On unavailable clip, fall back to idle. Do not treat looping mouth motion as audio-synchronized lip sync.

## Release gates
1. Add the actual GLB binary to repository using a binary-capable upload (do not base64 it into a Kotlin file).
2. Wire the Android renderer to the model and available clips, retaining existing teachers.
3. Verify mesh, bone weights, face, shoulder and knee on Android and Prisma3D.
4. Run Gradle build and signed APK verification.
5. Publish only after passing the above checks.

Current status: manifest and integration specification only; the binary and runtime changes have not yet been uploaded.
