# User-provided chair and window GLB — WordQuest AI

## Required binary asset

Place the **original** user-supplied file at exactly:

`assets/studio/chairs_and_window_set_-_game_ready.glb`

- File size: **19,765,636 bytes**
- SHA-256: `379c0537249af58dc1115184dc9f2257f045a1ba4bb9a266b7ab67898e8e8c81`
- glTF 2.0 (GLB), 2 meshes: `Asset_Asset_0`, `Plane_Plane_0`
- Four original embedded textures, seven nodes
- Scene size approximately 2 × 2 × 2.447 metres; the asset contains **both chair/window props** and original plane

## Author attribution (from the uploaded GLB)

- Original title: **Chairs and Window Set - Game Ready**
- Author: **General of Thailand** (https://sketchfab.com/Melonpolygons)
- License: **Creative Commons Attribution 4.0 (CC BY 4.0)**
- Source: https://sketchfab.com/3d-models/chairs-and-window-set-game-ready-2dac4adc767c41bbacdd26bff649d739

Keep this attribution with the redistributed GLB.

## Integration and safety

Run `python3 tools/v170/stage_user_chair_170.py` after the existing 1.0.69 source overlays.
The check fails for an altered asset or missing input, rather than silently using an imitation.
The original GLB bytes are copied into `source/app/src/main/assets/studio/`.
The Android renderer adds a distinct SceneView `ModelNode`, with world position
(2.45, -0.03, -1.40), independently of Lena's skeleton. The bundled 1.0.69 room
remains, but no generated chair is substituted for the supplied file.

User-reported broken greeting `02_Greeting_Gentle_Wave` is temporarily blocked:
the proven idle breathing animation is played instead. The original 201-bone rig
and all clips remain untouched in the model. A natural wave requires separate
retargeting and visual validation, not fabricated assurance.

**Publication gate:** Do not release v1.0.70 unless the original GLB has been
uploaded to the repository and the Android compilation/tests and signer pass.
The ChatGPT conversation attachment is **not automatically present in GitHub**.
