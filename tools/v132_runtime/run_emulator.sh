#!/usr/bin/env bash
# Run the existing mandatory upgrade QA on a disposable CI emulator.
# Own the emulator session and bound shutdown; no third-party wrapper waits.
set -euo pipefail
: "${ANDROID_HOME:?}" "${RUNNER_TEMP:?}"
QA="${1:?upgrade QA script required}"
APK="${2:?signed candidate required}"
PROOF="$RUNNER_TEMP/wq206-upgrade-passed.sha256"
LOG="$RUNNER_TEMP/wq206-emulator.log"
QA_LOG="$RUNNER_TEMP/wq206-upgrade-qa.log"
IMAGE='system-images;android-30;google_apis;x86_64'
AVD='wq206-upgrade'
# Pin the same absolute AVD directory for Java avdmanager and native emulator.
export ANDROID_USER_HOME="$RUNNER_TEMP/wq206-android-user"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
unset ANDROID_SDK_HOME
mkdir -p "$ANDROID_AVD_HOME"
rm -f "$PROOF"
test -s "$QA"
test -s "$APK"
test -r /dev/kvm && test -w /dev/kvm
SDKM="$(find "$ANDROID_HOME/cmdline-tools" -type f -name sdkmanager | head -1)"
AVDM="$(dirname "$SDKM")/avdmanager"
ADB="$ANDROID_HOME/platform-tools/adb"
EMU="$ANDROID_HOME/emulator/emulator"
test -x "$SDKM" && test -x "$AVDM" && test -x "$ADB"
# All child shells and Python subprocesses use the same verified ADB binary.
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export ADB
test "$(command -v adb)" = "$ADB"
adb version
echo "PASS: ADB is on PATH for all upgrade test subprocesses"
# GitHub runner licenses are normally accepted already. Limit SDK setup.
timeout --kill-after=15s 480s "$SDKM" --install emulator "$IMAGE" --channel=0
test -x "$EMU"
printf 'no\n' | timeout --kill-after=5s 60s "$AVDM" create avd --force -n "$AVD" --package "$IMAGE" --device pixel_4 --path "$ANDROID_AVD_HOME/$AVD.avd"
# The previous avdmanager invocation returned zero without a discoverable INI.
# Refuse to start an absent AVD; verify its directory and native discovery.
test -s "$ANDROID_AVD_HOME/$AVD.ini"
test -s "$ANDROID_AVD_HOME/$AVD.avd/config.ini"
grep -Fxq "path=$ANDROID_AVD_HOME/$AVD.avd" "$ANDROID_AVD_HOME/$AVD.ini"
timeout 20s "$EMU" -list-avds | tee "$RUNNER_TEMP/wq206-avd-list.log"
grep -Fxq "$AVD" "$RUNNER_TEMP/wq206-avd-list.log"
echo "PASS: avdmanager and emulator agree on $ANDROID_AVD_HOME/$AVD.ini"
timeout --kill-after=5s 20s "$ADB" start-server
# Separate session and redirected streams prevent orphan processes from
# holding the runner's output descriptors open after test completion.
setsid "$EMU" -port 5554 -avd "$AVD" -no-window -no-snapshot -noaudio \
    -no-boot-anim -gpu swiftshader_indirect </dev/null >"$LOG" 2>&1 &
emulator_pid=$!
cleanup() {
    status=$?
    trap - EXIT
    set +e
    echo "Closing disposable emulator (bounded shutdown)"
    timeout --kill-after=2s 8s "$ADB" -s emulator-5554 emu kill </dev/null >>"$LOG" 2>&1
    # Wait briefly for the session, then explicitly terminate remaining
    # QEMU/crashpad/netsimd children belonging to this emulator only.
    for _ in $(seq 1 8); do
        kill -0 -- "-$emulator_pid" 2>/dev/null || break
        sleep 1
    done
    if kill -0 -- "-$emulator_pid" 2>/dev/null; then
        kill -TERM -- "-$emulator_pid" 2>/dev/null
        sleep 3
    fi
    if kill -0 -- "-$emulator_pid" 2>/dev/null; then
        kill -KILL -- "-$emulator_pid" 2>/dev/null
    fi
    timeout --kill-after=2s 8s "$ADB" kill-server </dev/null >>"$LOG" 2>&1
    if [ "$status" -ne 0 ]; then
        rm -f "$PROOF"
        tail -n 100 "$LOG" || true
    fi
    echo "Emulator cleanup finished; QA exit code: $status"
    exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
export ANDROID_SERIAL=emulator-5554
# Check process liveness on every boot poll, rather than waiting ten minutes
# for a process which already exited. Bound every ADB call independently.
booted=false
deadline=$((SECONDS + 600))
while (( SECONDS < deadline )); do
    if ! kill -0 "$emulator_pid" 2>/dev/null; then
        echo "::error::Emulator exited before boot completed"
        tail -n 100 "$LOG"
        exit 1
    fi
    state="$(timeout --kill-after=2s 8s "$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" || state=""
    if [ "$state" = 1 ]; then booted=true; break; fi
    sleep 2
done
test "$booted" = true
timeout --kill-after=5s 30s "$ADB" shell input keyevent 82
echo "Android emulator boot completed"
# Keep every old QA test: installation, package/signature/UID continuity,
# app startup, Back behavior and three viewport/font screenshot checks.
timeout --kill-after=15s 720s bash "$QA" 2>&1 | tee "$QA_LOG"
grep -Fq 'PASS: actual Android mixed crossword gameplay opens' "$QA_LOG"
grep -Fq 'PASS: actual Android web quiz gameplay opens' "$QA_LOG"
grep -Fq 'PASS: signed Android candidate reached both web-engine gameplay screens' "$QA_LOG"
sha256sum "$APK" > "$PROOF"
echo "PASS: actual gameplay checks completed for exact signed APK"
