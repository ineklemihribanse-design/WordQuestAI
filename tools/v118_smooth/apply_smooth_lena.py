#!/usr/bin/env python3
"""Replace hard clip switches with a native Filament crossfade, preserving original GLB."""
from pathlib import Path
import hashlib,shutil
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'source/app/src/main/java/com/kelimebulmacasi/a1'
T=ROOT/'source/app/src/test/java/com/kelimebulmacasi/a1'
p=P/'LenaLive3DStage.kt';s=p.read_text()
def once(a,b):
    global s
    assert s.count(a)==1,('unexpected anchor',a[:150],s.count(a))
    s=s.replace(a,b,1)
once('    val autoCursor = remember(node) { mutableMapOf<String, Int>() }','''    val autoCursor = remember(node) { mutableMapOf<String, Int>() }
    val nativeBlend = remember(node) { Wq118LenaBlend() }
    val blendFailed = remember(node) { booleanArrayOf(false) }''')
once('''                model.playingAnimations.keys.toList().forEach(model::stopAnimation)
                model.playAnimation(index, speed = 1f, loop = loop)''','''                // Scene onFrame owns animation sampling. Never run the old
                // SceneView automatic player alongside the crossfade.
                nativeBlend.request(index, model.animator.getAnimationDuration(index),
                    loop, replay = manualClip != null)''')
once('''                    onFrame = { _ ->
                        if (!observedFrame) observedFrame = true
                    },''','''                    onFrame = { frameTimeNanos ->
                        if (!observedFrame) observedFrame = true
                        if (!blendFailed[0]) {
                            runCatching {
                                nativeBlend.frame(frameTimeNanos)?.let { sample ->
                                    val animator = node.animator
                                    animator.applyAnimation(sample.current, sample.time)
                                    sample.previous?.let { previous ->
                                        animator.applyCrossFade(previous, sample.previousTime, sample.alpha)
                                    }
                                    animator.updateBoneMatrices()
                                }
                            }.onFailure {
                                blendFailed[0] = true
                                Log.e("WQLenaNative", "Native crossfade failed", it)
                                onRecoverableError()
                            }
                        }
                    },''')
s=s.replace('// No speed changes, no overlapping animations, no bone edits. Switch only after the full clip.',
    '// Original-speed clips; native crossfade preserves the previous pose at every switch.')
assert 'model.playAnimation(' not in s and 'model::stopAnimation' not in s
assert s.count('animator.applyCrossFade(')==1 and 'spokenReply' in s
assert 'autoAnimate = false' in s
asset=ROOT/'source/app/src/main/assets/avatars/lena/Lena_Mixamo_38_Stable.glb'
assert hashlib.sha256(asset.read_bytes()).hexdigest()=='cb033dc3503f01e3fa64971c2bbe40b1260e7d9a96f7860796a32400328ad26f'
p.write_text(s)
shutil.copyfile(ROOT/'tools/v118_smooth/Wq118LenaBlend.kt',P/'Wq118LenaBlend.kt')
shutil.copyfile(ROOT/'tools/v118_smooth/Wq118LenaBlendTest.kt',T/'Wq118LenaBlendTest.kt')
print('PASS: native frame-clock crossfade installed; original 38-clip GLB unchanged')
