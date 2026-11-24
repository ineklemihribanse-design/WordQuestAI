#!/usr/bin/env bash
set -euo pipefail
D="$RUNNER_TEMP/wq118-device"
PKG=com.kelimebulmacasi.a1.practice.fresh
BASE="$D/old.apk"
NEW="$D/target.apk"
SIGNER="$ANDROID_HOME/build-tools/35.0.0/lib/apksigner.jar"
AAPT="$ANDROID_HOME/build-tools/35.0.0/aapt"
CERT=29f9449cef86e511bb8256ca1db58580ff88dd1badd034ff79b81c6df662bce6
for apk in "$BASE" "$NEW"; do
  java -jar "$SIGNER" verify --print-certs "$apk" > "$D/$(basename "$apk").cert"
  grep -Fiq "Signer #1 certificate SHA-256 digest: $CERT" "$D/$(basename "$apk").cert"
  "$AAPT" dump badging "$apk" > "$D/$(basename "$apk").badging"
  grep -Fq "name='$PKG'" "$D/$(basename "$apk").badging"
done
grep -Fq "versionCode='143'" "$D/old.apk.badging"
grep -Fq "versionCode='144'" "$D/target.apk.badging"
adb install -r "$BASE"
adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
sleep 8
adb shell pidof -s "$PKG"
if grep -Fq 'android.permission.CAMERA' <("$AAPT" dump permissions "$BASE"); then
  adb shell pm grant "$PKG" android.permission.CAMERA
fi
adb shell dumpsys package "$PKG" > "$D/before.txt"
adb shell pm path "$PKG" | tr -d '\r' > "$D/installed-path.txt"
test "$(wc -l < "$D/installed-path.txt")" = 1
installed="$(sed -n 's/^package://p' "$D/installed-path.txt")"
test -n "$installed"
adb pull "$installed" "$D/installed.apk"
cmp "$D/installed.apk" "$BASE"
python3 - "$D" <<'PY'
import json,sys,subprocess
from pathlib import Path
d=Path(sys.argv[1]);r=json.loads((d/'report.json').read_text())
assert r['eligible']
p=r['delta']
cmd=['java','-cp',str(d/'classes'),'com.kelimebulmacasi.a1.WqDeltaApplier',str(d/'installed.apk'),str(d/'patch.gz'),str(d/'reconstructed.apk'),str(p['baseSizeBytes']),str(p['sizeBytes']),str(p['targetSizeBytes']),p['baseSha256'],p['sha256'],p['targetSha256']]
subprocess.run(cmd,check=True)
PY
cmp "$D/reconstructed.apk" "$NEW"
java -jar "$SIGNER" verify --print-certs "$D/reconstructed.apk" > "$D/reconstructed.cert"
grep -Fiq "Signer #1 certificate SHA-256 digest: $CERT" "$D/reconstructed.cert"
adb shell am force-stop "$PKG"
adb install -r "$D/reconstructed.apk"
adb shell dumpsys package "$PKG" > "$D/after.txt"
python3 - "$D" "$PKG" <<'PY'
import sys,importlib.util
from pathlib import Path
d=Path(sys.argv[1])
spec=importlib.util.spec_from_file_location('meta','tools/v190/verify_upgrade_metadata_190.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
a=m.package_snapshot((d/'before.txt').read_text(),sys.argv[2])
b=m.package_snapshot((d/'after.txt').read_text(),sys.argv[2])
assert (a['version'],b['version'])==(143,144)
for k in ('uid','first_installed','data_dir'):
    assert a[k]==b[k],(k,a[k],b[k])
if a['camera_permission']=='true': assert b['camera_permission']=='true'
print('PASS: Android package UID, installation time, data path and camera permission continuity')
PY
adb logcat -c
adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
sleep 12
adb shell pidof -s "$PKG"
adb exec-out screencap -p > "$D/startup.png"
python3 - "$D/startup.png" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1]); assert p.stat().st_size>10000 and p.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
PY
echo 'PASS: exact installed v1.0.117 reconstructed and upgraded to v1.0.118'
echo 'PASS: original Android UID unchanged, signatures match and app process stays alive.'
echo 'LIMITATION: host Java reconstructs pulled installed APK; in-app download, real device rendering and private user database content are not yet tested.'
