#!/usr/bin/env python3
"""v1.0.64: replace only Lena APK asset with true Prisma3D object materials.

The uploaded 'Prisma3D ... .zip' is an OBJ text file matching the repository's
original Lena v6 GLB exactly, including vertex and triangle order. The model
already has 9 independent component boundaries. Rebuild FROM ORIGINAL, never
from overpainted v163. Keep 201-bone rig, 16 body clips, AI credentials and
user data. Add new eye blink + lip-local TTS morphs without fake objects.
"""
from pathlib import Path
import hashlib,json,subprocess,sys,tempfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"source/app"
PKG=APP/"src/main/java/com/kelimebulmacasi/a1"
TEST=APP/"src/test/java/com/kelimebulmacasi/a1"
SOURCE=ROOT/"assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
GLB=APP/"src/main/assets/avatars/lena/Lena_Prism3D_AnimationPack_v6.glb"
META=GLB.parent/"animation-map.json"
GRADLE=APP/"build.gradle.kts"
OLD="Lena_V162_Face_"
NEW="Lena_V164_Face_"

def once(s,a,b,label):
 if s.count(a)!=1:raise AssertionError(f"{label}: expected exactly one, found {s.count(a)}")
 return s.replace(a,b,1)

def main():
 g=GRADLE.read_text("utf-8")
 assert g.count("versionCode = 88")==1
 assert g.count('versionName = "1.0.63-lena-eyes-brown-hair"')==1
 assert 'applicationIdSuffix = ".fresh"' in g
 for name in ("Teacher2DPortrait.kt","ArenaApprovedHome.kt",
              "VocabularyNeonHome.kt","LenaLive3DStage.kt",
              "LenaIsolatedVoiceTeacherActivity.kt","TeacherCredentialBridgeProvider.kt"):
  assert (PKG/name).is_file(),name
 assert SOURCE.is_file() and GLB.is_file() and META.is_file()
 sha="548d70117df4599300c77dc3fde3c72a2111f78ff582e741862c25fcdf364617"
 assert hashlib.sha256(SOURCE.read_bytes()).hexdigest()==sha
 before=json.loads(META.read_text("utf-8"))
 assert before["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()
 assert before["baseAssetSha256"]==sha

 # Directly stage the original 9 components with exact source triangle ranges,
 # rather than the v163 white/floating-eye/black-mouth compound model.
 with tempfile.TemporaryDirectory(prefix="lena-original-") as tmp:
  output=Path(tmp)/"Lena_original_parts.glb"
  subprocess.run([sys.executable,
                  str(ROOT/"tools/v164/build_lena_prisma_parts.py"),
                  str(SOURCE),str(output),str(META)],check=True)
  GLB.write_bytes(output.read_bytes())

 subprocess.run([sys.executable,str(ROOT/"tools/verify_lena_glb.py"),str(GLB)],check=True)
 after=json.loads(META.read_text("utf-8"))
 assert after["sha256"]==hashlib.sha256(GLB.read_bytes()).hexdigest()
 assert after["baseAssetSha256"]==sha
 assert after["originalPrismaObjSha256"]=="9ac3147dcbd838eb562e47a1ad580af59603124b3cdfd0a168cf36eac57786f1"

 # Existing renderer already blends face morph loop with 16 original
 # body actions, selected by TTS speaking/listening state. Change only names.
 for f in (PKG/"LenaFaceAnimationClips.kt",
           TEST/"LenaFaceAnimationClipsTest.kt"):
  s=f.read_text("utf-8")
  assert s.count(OLD+"IdleBlink")>=1 and s.count(OLD+"TalkBlink")>=1
  f.write_text(s.replace(OLD,NEW),"utf-8")
 assert NEW+"TalkBlink" in (PKG/"LenaFaceAnimationClips.kt").read_text()
 # Smooth original face/skin normals in GLB. Keep HDR light reasonable
 # to show dark hair and tan skin without washed-out highlights.
 stage=PKG/"LenaLive3DStage.kt"
 s=stage.read_text("utf-8")
 s=once(s,"intensity = 50_000f","intensity = 38_000f","balanced SceneView lighting")
 stage.write_text(s,"utf-8")

 g=once(g,"versionCode = 88","versionCode = 89","increment version")
 g=once(g,'versionName = "1.0.63-lena-eyes-brown-hair"',
        'versionName = "1.0.64-prisma-original-look"',"set version")
 GRADLE.write_text(g,"utf-8")
 print("v1.0.64: all nine original OBJ mesh regions restored as separate PBR materials")
 print("No floating eye/lip overlays, no mesh coordinate guessing; preserved Alex/Arena/Word Memory and saved keys")
 print("Compiled Lena GLB bytes",GLB.stat().st_size)

if __name__=="__main__":main()
