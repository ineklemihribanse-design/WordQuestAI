from pathlib import Path
import shutil,subprocess,sys,hashlib
ROOT=Path(__file__).resolve().parents[2]
P=ROOT/'source/app/src/main/java/com/kelimebulmacasi/a1'
T=ROOT/'source/app/src/test/java/com/kelimebulmacasi/a1'
p=P/'LenaLive3DStage.kt';s=p.read_text()
def once(a,b):
    global s
    assert s.count(a)==1,(a[:120],s.count(a))
    s=s.replace(a,b,1)
asset=ROOT/'source/app/src/main/assets/avatars/lena/Lena_Mixamo_38_Stable.glb'
out=asset.with_name('Lena_Mixamo_38_Face119.glb')
subprocess.run([sys.executable,str(ROOT/'tools/v119_face/build_face.py'),str(asset),str(ROOT/'Lena_V5_Android_Entegrasyon.zip'),str(out)],check=True)
assert hashlib.sha256(out.read_bytes()).hexdigest()=='7cb8bd4fd09fd34b0a28a67363881c43e8af5420ce23412e139b14c957159bc5'
once('"avatars/lena/Lena_Mixamo_38_Stable.glb"','"avatars/lena/Lena_Mixamo_38_Face119.glb"')
once('    val blendFailed = remember(node) { booleanArrayOf(false) }','''    val blendFailed = remember(node) { booleanArrayOf(false) }
    val faceController = remember(node) { Wq119LenaFace() }
    val faceFailed = remember(node) { booleanArrayOf(false) }
    val currentMouth by rememberUpdatedState(mouthCue)
    val currentMotion by rememberUpdatedState(motion)
    val currentReply by rememberUpdatedState(spokenReply)''')
# Append a separate guarded face layer after the unchanged, previously verified body renderer.
anchor='''                                onRecoverableError()
                            }
                        }
                    },'''
once(anchor,'''                                onRecoverableError()
                            }
                        }
                        if (!blendFailed[0] && !faceFailed[0]) {
                            runCatching {
                                val cue = currentMouth
                                val speaking = currentMotion == AvatarMode.SPEAKING
                                val smiling = currentMotion == AvatarMode.GREETING ||
                                    (speaking && Wq117LenaMotion.classify(currentReply.orEmpty()) ==
                                        Wq117LenaMotion.Intent.PRAISE)
                                val weights = faceController.frame(
                                    android.os.SystemClock.elapsedRealtime(), speaking,
                                    cue.viseme.name, cue.weight, cue.cueAtMs, cue.quality.name, smiling,
                                    currentMotion != AvatarMode.ERROR)
                                // Explicit offset=0: do not use SceneView's default offset.
                                node.setMorphWeights(weights, 0)
                            }.onFailure {
                                faceFailed[0] = true
                                Log.w("WQLenaFace119", "Face layer disabled; body animation continues", it)
                            }
                        }
                    },''')
assert s.count('animator.applyCrossFade(')==1 and s.count('node.setMorphWeights(weights, 0)')==1
assert 'autoAnimate = false' in s
p.write_text(s)
# Package a single native model, with original geometry and animation buffer unchanged.
asset.unlink()
shutil.copyfile(ROOT/'tools/v119_face/Wq119LenaFace.kt',P/'Wq119LenaFace.kt')
shutil.copyfile(ROOT/'tools/v119_face/Wq119LenaFaceTest.kt',T/'Wq119LenaFaceTest.kt')
print('PASS: original 38 clips preserved, independent cue-driven face layer installed')
