#!/usr/bin/env bash
# GitHub-hosted emulator QA for an already installed WordQuest AI 1.0.74.
# This script NEVER publishes an APK or touches a user's physical device.
set -euo pipefail

PKG="com.kelimebulmacasi.a1.practice.fresh"
OLD_URL="https://github.com/ineklemihribanse-design/WordQuestAI/releases/download/v1.0.74/WordQuestAI.apk"
OLD="$RUNNER_TEMP/wq175-original-v174.apk"
NEW="source/app/build/outputs/apk/offline/WordQuestAI-v1.0.75-signed-qa.apk"
AAPT="$ANDROID_HOME/build-tools/35.0.0/aapt"
SIGNER="$ANDROID_HOME/build-tools/35.0.0/lib/apksigner.jar"
EXPECTED_SHA="29f9449cef86e511bb8256ca1db58580ff88dd1badd034ff79b81c6df662bce6"

test -s "$NEW"
test -s "$SIGNER"
curl --fail --location --retry 3 --connect-timeout 15 --max-time 240 \
    --silent --show-error "$OLD_URL" --output "$OLD"
test -s "$OLD"
for APK in "$OLD" "$NEW"; do
    echo "Checking package, signer and integrity for $(basename "$APK")"
    unzip -tq "$APK" >/dev/null
    "$AAPT" dump badging "$APK" | grep -q "package: name='$PKG'"
    java -jar "$SIGNER" verify --print-certs "$APK" > "$RUNNER_TEMP/wq175-apk-cert.txt"
    grep -qi "Signer #1 certificate SHA-256 digest: $EXPECTED_SHA" \
        "$RUNNER_TEMP/wq175-apk-cert.txt"
done
"$AAPT" dump badging "$OLD" | grep -q "versionCode='100'"
"$AAPT" dump badging "$NEW" | grep -q "versionCode='101'"
adb wait-for-device
adb shell input keyevent 82 || true
echo "Install real published signed v1.0.74 from GitHub releases"
adb install -r "$OLD"
adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
sleep 7
old_uid="$(adb shell dumpsys package "$PKG" | sed -n 's/^[[:space:]]*userId=\([0-9][0-9]*\).*/\1/p' | head -1 | tr -d '\r')"
test -n "$old_uid"
adb shell am force-stop "$PKG"
echo "Upgrade in place; never uninstall, clear app data, or change Android user"
adb install -r "$NEW"
new_uid="$(adb shell dumpsys package "$PKG" | sed -n 's/^[[:space:]]*userId=\([0-9][0-9]*\).*/\1/p' | head -1 | tr -d '\r')"
test -n "$new_uid"
test "$old_uid" = "$new_uid"
adb shell dumpsys package "$PKG" | grep -q 'versionCode=101'
adb logcat -c || true
adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
sleep 12
if ! adb shell pidof -s "$PKG" >/dev/null; then
    echo "::error::WordQuest AI process not running after upgrade."
    adb logcat -d -t 400 | grep -E 'FATAL EXCEPTION|AndroidRuntime|SIGSEGV|SIGABRT|kelimebulmacasi' | tail -50 || true
    exit 1
fi
adb exec-out screencap -p > "$RUNNER_TEMP/wq175-emulator-startup.png"
python3 - "$RUNNER_TEMP/wq175-emulator-startup.png" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1])
assert p.stat().st_size>10000, "Emulator screenshot not captured"
assert p.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
print("PASS: emulator produced a valid startup screenshot",p.stat().st_size,"bytes")
PY
echo "PASS: old v1.0.74 -> v1.0.75 in-place upgrade on a clean Android emulator."
echo "PASS: original Android UID unchanged, signatures match and app process stays alive."
echo "LIMITATION: emulator package checks do not replace real user-data, camera, tablet and online multiplayer QA."
