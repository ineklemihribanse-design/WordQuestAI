# WordQuest AI 1.0.27 — Emma model: visual acceptance gate

## Non-negotiable target
Use the user's supplied reference images as visual direction: **polished, adult, approachable, semi-realistic stylized 3D humanoid**. A rigged model merely loading or passing an automated build is **not** evidence of premium appearance.

## Why this candidate
Evaluate `Microsoft Rocketbox / Avatar Studio emma_casual.glb` as a source of correctly formed human anatomy, textured hair, adult clothing and high-quality face expressions. Model and code are MIT. Retain the full Microsoft license notice in APK assets if used.

## Visual tests (must be inspected, not inferred)
- [ ] Full-body front: head-to-body proportion natural, feet visible, calm arms not T-pose
- [ ] Side/3/4: hair, clothing, nose, jaw and torso look coherent, no gaps or clipping
- [ ] Face close-up: distinct brows, textured hair, symmetric eyes, natural skin, lips
- [ ] Appropriate for an adult language tutor, not a mascot, toy, cartoon child or rigid mannequin
- [ ] Reference fidelity: warm semi-realistic stylization and facial expression; a photorealistic model may fail this even if objectively detailed
- [ ] Under soft studio light, face is bright and distinguishable against teal background
- [ ] No unlicensed derivative assets or copyrighted brand/game characters

## Technical tests
- [ ] Valid GLB 2.0 with MIT license provenance and pinned origin
- [ ] One or more skinned skeletons, named animations and facial morph targets
- [ ] Neutral resting / idle animation actively playing at scene start, never raw rest T-pose
- [ ] Speaking/listening/think/greet motion tested; transitions are stable
- [ ] Pinch, rotate, face zoom and reset keep character onscreen
- [ ] 2D fallback if GPU/model initialization fails
- [ ] On-device screenshot & short motion check, 30 FPS target on representative Android
- [ ] No app data, signing key, package ID or history changes

## Release gate
The current preview pipeline is a **candidate-only branch** and must not be merged into `main` or published through the signed updater until the user sees full-body and face close-ups and the overall quality is accepted. The previous VRoid cat-ear/anime candidates were rejected.
