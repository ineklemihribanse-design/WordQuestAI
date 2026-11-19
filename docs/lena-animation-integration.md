# Lena — Prisma3D animation integration (development)

Status: asset reviewed locally, application integration NOT yet implemented.

## Source asset
User-supplied Lena model must remain outside the public repository until explicit redistribution permission is established. Do not commit the OBJ or GLB binaries. Keep Alex and existing avatar files unchanged.

## Verified GLB v6 structure
- glTF 2 binary (.glb), one skin, 232 nodes
- 16 named animation clips (not all independently authored)
- Clips: 01_Idle_Natural_Breathing, 02_Greeting_Gentle_Wave, 03_Talking_Hand_Gestures, 04_Listening_Nod, 05_Subtle_Weight_Shift, 06_Face_Soft_Blink, 07_Face_Speech_Cycle, 08_Face_Soft_Smile, 09_Resting_With_Blinks, 10_Wave_With_Smile, 11_Talking_With_Blinks, 12_Listening_With_Blinks, 13_Thinking_Softly, 14_Expressive_Talking, 15_Cheerful_Greeting, 16_Relaxed_Smile.

## Next implementation
1. Keep Lena as optional/selectable teacher independent of Alex.
2. Import GLB locally in Prisma3D and validate clips, skinning, shoulder/knee/face deformation; avoid assuming all imported clips work.
3. Implement Android animation state machine: idle, listen, speak, greet; prevent abrupt transitions; select clips only if present.
4. Validate facial deformations and voice-to-mouth timing on-device before claiming lip sync.
5. Add walk/sit/stand only after actual authoring and testing; do not rename existing clips as new motion.
6. Keep app UI and existing teacher untouched.

No APK or on-device validation has been performed for this document-only change.
