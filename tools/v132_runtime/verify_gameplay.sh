#!/usr/bin/env bash
set -euo pipefail
D="${WQ_RELEASE_DIR:?}"
APK="$D/target.apk"
adb install -r "$APK"
adb shell am start -n com.kelimebulmacasi.a1.practice.fresh/com.kelimebulmacasi.a1.MainActivity
sleep 10
adb shell pidof -s com.kelimebulmacasi.a1.practice.fresh
python3 ../tools/v132_runtime/qa_engine_ui.py
adb logcat -d -t 2000 > "$D/android-logcat.txt"
echo 'PASS: signed Android candidate reached both web-engine gameplay screens'
