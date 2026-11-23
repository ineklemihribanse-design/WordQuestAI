#!/usr/bin/env bash
# GitHub-hosted emulator QA for an already installed WordQuest AI 1.0.86.
# This script NEVER publishes an APK or touches a user's physical device.
set -euo pipefail

PKG="com.kelimebulmacasi.a1.practice.fresh"
OLD_URL="https://github.com/ineklemihribanse-design/WordQuestAI/releases/download/v1.0.88/WordQuestAI.apk"
OLD="$RUNNER_TEMP/wq189-original-v188.apk"
NEW="source/app/build/outputs/apk/offline/WordQuestAI-v1.0.89-signed-qa.apk"
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
    "$AAPT" dump badging "$APK" > "$RUNNER_TEMP/wq175-$(basename "$APK").badging"
    grep -Fq "package: name='$PKG'" "$RUNNER_TEMP/wq175-$(basename "$APK").badging"
    java -jar "$SIGNER" verify --print-certs "$APK" > "$RUNNER_TEMP/wq175-apk-cert.txt"
    grep -qi "Signer #1 certificate SHA-256 digest: $EXPECTED_SHA" \
        "$RUNNER_TEMP/wq175-apk-cert.txt"
done
grep -Fq "versionCode='114'" "$RUNNER_TEMP/wq175-$(basename "$OLD").badging"
grep -Fq "versionCode='115'" "$RUNNER_TEMP/wq175-$(basename "$NEW").badging"
if grep -Fq 'application-debuggable' "$RUNNER_TEMP/wq175-$(basename "$NEW").badging"; then
    echo "::error::The user-upgrade APK is debuggable; refuse production QA."
    exit 1
fi
adb wait-for-device
adb shell input keyevent 82 || true
echo "Install real published signed v1.0.88 from GitHub releases"
adb install -r "$OLD"
adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
sleep 7
old_uid="$(adb shell dumpsys package "$PKG" | sed -n 's/^[[:space:]]*userId=\([0-9][0-9]*\).*/\1/p' | head -1 | tr -d '\r')"
test -n "$old_uid"
# A disposable emulator is safe for a synthetic user permission grant.
# If published v1.0.88 requests CAMERA, verify that a granted permission
# remains granted after upgrading to v1.0.89.
"$AAPT" dump permissions "$OLD" > "$RUNNER_TEMP/wq175-v174-permissions.txt"
if grep -Fq 'android.permission.CAMERA' "$RUNNER_TEMP/wq175-v174-permissions.txt"; then
    echo "Grant camera on the test emulator to verify Android permission continuity"
    adb shell pm grant "$PKG" android.permission.CAMERA
fi
adb shell dumpsys package "$PKG" > "$RUNNER_TEMP/wq175-original-package.txt"
adb shell am force-stop "$PKG"
echo "Upgrade in place; never uninstall, clear app data, or change Android user"
adb install -r "$NEW"
new_uid="$(adb shell dumpsys package "$PKG" | sed -n 's/^[[:space:]]*userId=\([0-9][0-9]*\).*/\1/p' | head -1 | tr -d '\r')"
test -n "$new_uid"
test "$old_uid" = "$new_uid"
adb shell dumpsys package "$PKG" > "$RUNNER_TEMP/wq175-upgraded-package.txt"
grep -q 'versionCode=115' "$RUNNER_TEMP/wq175-upgraded-package.txt"
python3 tools/v189/verify_upgrade_metadata_189.py \
    "$RUNNER_TEMP/wq175-original-package.txt" \
    "$RUNNER_TEMP/wq175-upgraded-package.txt" "$PKG"
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
# Exercise screen-size, font-scale and rotation compatibility without modifying app data.
# Capture screenshots for human review; a screenshot is not a visual-design pass.
qa_dir="$RUNNER_TEMP/wq175-responsive-qa"
mkdir -p "$qa_dir"
original_size="$(adb shell wm size | sed -n 's/^Physical size: //p' | tr -d '\r')"
original_density="$(adb shell wm density | sed -n 's/^Physical density: //p' | tr -d '\r')"
original_font="$(adb shell settings get system font_scale | tr -d '\r')"
restore_display() {
    adb shell wm size reset >/dev/null 2>&1 || true
    adb shell wm density reset >/dev/null 2>&1 || true
    if [[ -z "$original_font" || "$original_font" == "null" ]]; then
        adb shell settings delete system font_scale >/dev/null 2>&1 || true
    else
        adb shell settings put system font_scale "$original_font" >/dev/null 2>&1 || true
    fi
    adb shell settings put system accelerometer_rotation 1 >/dev/null 2>&1 || true
    adb shell settings put system user_rotation 0 >/dev/null 2>&1 || true
}
trap restore_display EXIT
echo "Original emulator display: $original_size, density: $original_density, font: $original_font"
for layout in small_phone large_text tablet_landscape; do
    case "$layout" in
        small_phone)
            # 720 physical pixels at 320 DPI = 360 dp logical width.
            expected_pixels="720x1280"
            adb shell wm size 720x1280
            adb shell wm density 320
            adb shell settings put system font_scale 1.0
            adb shell settings put system accelerometer_rotation 0
            adb shell settings put system user_rotation 0
            ;;
        large_text)
            expected_pixels="720x1280"
            adb shell wm size 720x1280
            adb shell wm density 320
            adb shell settings put system font_scale 1.4
            adb shell settings put system accelerometer_rotation 0
            adb shell settings put system user_rotation 0
            ;;
        tablet_landscape)
            expected_pixels="1280x800"
            # wm size already sets a landscape base surface; rotating by 90
            # degrees here made screenshots 800x1280 and falsely failed QA.
            adb shell wm size 1280x800
            adb shell wm density 240
            adb shell settings put system font_scale 1.0
            adb shell settings put system accelerometer_rotation 0
            # Android 11 (API 30) emulator has no 'wm user-rotation' subcommand.
            # Rotation 0 maintains the requested 1280x800 landscape base display.
            adb shell settings put system user_rotation 0
            ;;
    esac
    adb shell am force-stop "$PKG"
    adb shell am start -n "$PKG/com.kelimebulmacasi.a1.MainActivity"
    sleep 10
    if ! adb shell pidof -s "$PKG" >/dev/null; then
        echo "::error::App stopped during $layout responsive QA"
        adb logcat -d -t 200 | grep -E 'FATAL EXCEPTION|AndroidRuntime|SIGSEGV|SIGABRT' | tail -35 || true
        exit 1
    fi
    # Re-capture after configuration changes if Android briefly returns a
    # truncated screenshot. A valid 1-KB PNG is not a test failure.
    image_file="$qa_dir/$layout.png"
    screenshot_ok=false
    for attempt in 1 2 3 4 5; do
        if adb exec-out screencap -p > "$image_file" &&
           python3 tools/v175/verify_emulator_screenshot_175.py "$image_file" \
               --require-ui "--expect=$expected_pixels"; then
            screenshot_ok=true
            break
        fi
        echo "::warning::$layout screenshot invalid on attempt $attempt; rechecking"
        sleep 3
    done
    if [[ "$screenshot_ok" != true ]]; then
        echo "::error::$layout screenshot had no real app content after five attempts."
        adb logcat -d -t 300 | grep -E 'FATAL EXCEPTION|AndroidRuntime|SIGSEGV|SIGABRT|Screenshot|Compose|Exception' | tail -45 || true
        exit 1
    fi

done
restore_display
trap - EXIT
echo "PASS: three emulator viewport/font smoke tests; manual visual review still required."
echo "PASS: old v1.0.88 -> v1.0.89 in-place upgrade on a clean Android emulator."
echo "PASS: original Android UID unchanged, signatures match and app process stays alive."
echo "LIMITATION: emulator package checks do not replace real user-data, camera, tablet and online multiplayer QA."
