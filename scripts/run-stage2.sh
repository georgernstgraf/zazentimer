#!/bin/bash
set -euo pipefail

if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME}" ]; then
	echo "=== Using ANDROID_HOME=$ANDROID_HOME ==="
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT}" ]; then
	export ANDROID_HOME="$ANDROID_SDK_ROOT"
	echo "=== Using ANDROID_SDK_ROOT=$ANDROID_HOME ==="
elif [ -d "$HOME/Android/Sdk" ]; then
	export ANDROID_HOME="$HOME/Android/Sdk"
	echo "=== Using local SDK at $ANDROID_HOME ==="
elif [ -d "/opt/android-sdk" ]; then
	export ANDROID_HOME="/opt/android-sdk"
	echo "=== Using VPS SDK at $ANDROID_HOME ==="
else
	echo "ERROR: Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
	exit 1
fi
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -n "$(git -C "$PROJECT_DIR" status --porcelain)" ]; then
	echo "ERROR: Git repository is not clean. Commit or stash changes before running."
	git -C "$PROJECT_DIR" status --short
	exit 1
fi

XVFB_PID=""
UNIT_RESULT=0
API29_RESULT=0
API35_RESULT=0

cleanup() {
	if [ -n "$XVFB_PID" ]; then
		kill "$XVFB_PID" 2>/dev/null || true
		wait "$XVFB_PID" 2>/dev/null || true
	fi
	adb devices | grep -q "emulator" && adb -s emulator-5554 emu kill 2>/dev/null || true
}
trap cleanup EXIT

if [ -z "${DISPLAY:-}" ]; then
	echo "=== DISPLAY not set — starting Xvfb on :99 ==="
	Xvfb :99 -screen 0 1080x1920x24 &
	XVFB_PID=$!
	export DISPLAY=:99
	sleep 2
	echo "Xvfb started (PID $XVFB_PID)"
else
	echo "=== DISPLAY=$DISPLAY — using existing display ==="
fi

wait_for_emulator() {
	local serial="$1"
	local timeout_sec="${2:-300}"
	local elapsed=0
	echo "Waiting for emulator $serial to boot..."
	adb -s "$serial" wait-for-device
	while [ $elapsed -lt $timeout_sec ]; do
		local boot_done
		boot_done=$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
		if [ "$boot_done" = "1" ]; then
			echo "Emulator $serial booted (${elapsed}s)"
			adb -s "$serial" shell svc power stayon true
			adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
			adb -s "$serial" shell input keyevent KEYCODE_MENU
			adb -s "$serial" shell input keyevent KEYCODE_HOME
			sleep 5
			return 0
		fi
		sleep 5
		elapsed=$((elapsed + 5))
	done
	echo "ERROR: Emulator $serial did not boot within ${timeout_sec}s"
	return 1
}

kill_emulator() {
	local serial="$1"
	echo "Killing emulator $serial..."
	adb -s "$serial" emu kill 2>/dev/null || true
	sleep 2
}

clean_device_packages() {
	local serial="$1"
	echo "Cleaning stale packages on $serial..."
	for pkg in \
		de.gaffga.android.zazentimer \
		de.gaffga.android.zazentimer.test \
		at.priv.graf.zazentimer \
		at.priv.graf.zazentimer.test; do
		adb -s "$serial" uninstall "$pkg" >/dev/null 2>&1 || true
	done
}

dismiss_anr_dialog() {
	local serial="$1"
	for attempt in 1 2 3; do
		local anr_window
		anr_window=$(adb -s "$serial" shell "dumpsys window windows" 2>/dev/null | grep -c "Application Error\|isn't responding\|is not responding" || true)
		if [ "$anr_window" -eq 0 ] 2>/dev/null; then
			return 0
		fi
		echo "ANR dialog detected on $serial (attempt $attempt) — dismissing..."
		adb -s "$serial" shell input keyevent KEYCODE_DPAD_RIGHT 2>/dev/null || true
		adb -s "$serial" shell input keyevent KEYCODE_ENTER 2>/dev/null || true
		sleep 3
	done
}

resolve_avd() {
	local api_level=$1
	local avd_list
	avd_list=$($ANDROID_HOME/emulator/emulator -list-avds 2>/dev/null)

	if echo "$avd_list" | grep -qx "test_api${api_level}"; then
		echo "test_api${api_level}"
		return 0
	fi

	if echo "$avd_list" | grep -qx "Medium_Phone_API_${api_level}"; then
		echo "Medium_Phone_API_${api_level}"
		return 0
	fi

	local match
	match=$(echo "$avd_list" | grep "${api_level}" | head -1)
	if [ -n "$match" ]; then
		echo "$match"
		return 0
	fi

	echo "ERROR: No AVD found for API $api_level. Available:" >&2
	echo "$avd_list" >&2
	return 1
}

# ──────────────────────────────────────────────
# Unit Tests
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Unit Tests"
echo "========================================="
cd "$PROJECT_DIR"
set +e
./gradlew test --no-daemon
UNIT_RESULT=$?
set -e

if [ $UNIT_RESULT -ne 0 ]; then
	echo "FAIL: Unit tests failed — skipping instrumented tests"
else
	echo "PASS: Unit tests"

	# ──────────────────────────────────────────────
	# API 29 — Gradle connectedDebugAndroidTest
	# ──────────────────────────────────────────────
	echo ""
	echo "========================================="
	AVD29=$(resolve_avd 29) || {
		API29_RESULT=1
		echo "FAIL: Could not find AVD for API 29"
	}
	echo "  API 29 — Starting emulator ($AVD29)"
	echo "========================================="
	$ANDROID_HOME/emulator/emulator \
		-avd "$AVD29" \
		-no-snapshot \
		-gpu swiftshader_indirect \
		-noaudio \
		-no-boot-anim \
		-memory 4096 &
	API29_SERIAL="emulator-5554"

	if ! wait_for_emulator "$API29_SERIAL"; then
		echo "FAIL: API 29 emulator did not boot"
		API29_RESULT=1
	else
		clean_device_packages "$API29_SERIAL"
		dismiss_anr_dialog "$API29_SERIAL"
		adb -s "$API29_SERIAL" shell svc power stayon true 2>/dev/null || true
		adb -s "$API29_SERIAL" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
		adb -s "$API29_SERIAL" shell input keyevent KEYCODE_HOME 2>/dev/null || true
		sleep 5

		echo ""
		echo "========================================="
		echo "  API 29 — Running instrumented tests"
		echo "========================================="
		set +e
		cd "$PROJECT_DIR"
		./gradlew connectedDebugAndroidTest --no-daemon
		API29_RESULT=$?
		set -e
	fi

	kill_emulator "$API29_SERIAL"

	# ──────────────────────────────────────────────
	# API 35 — am instrument (UTP workaround)
	# ──────────────────────────────────────────────
	echo ""
	echo "========================================="
	echo "  API 35 — Building APKs"
	echo "========================================="
	cd "$PROJECT_DIR"
	./gradlew assembleDebug assembleDebugAndroidTest --no-daemon

	echo ""
	echo "========================================="
	AVD35=$(resolve_avd 35) || {
		API35_RESULT=1
		echo "FAIL: Could not find AVD for API 35"
	}
	echo "  API 35 — Starting emulator ($AVD35)"
	echo "========================================="
	$ANDROID_HOME/emulator/emulator \
		-avd "$AVD35" \
		-no-snapshot \
		-gpu swiftshader_indirect \
		-noaudio \
		-no-boot-anim \
		-memory 4096 &
	API35_SERIAL="emulator-5554"

	if ! wait_for_emulator "$API35_SERIAL"; then
		echo "FAIL: API 35 emulator did not boot"
		API35_RESULT=1
	else
		clean_device_packages "$API35_SERIAL"
		dismiss_anr_dialog "$API35_SERIAL"
		adb -s "$API35_SERIAL" shell svc power stayon true 2>/dev/null || true
		adb -s "$API35_SERIAL" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
		adb -s "$API35_SERIAL" shell input keyevent KEYCODE_HOME 2>/dev/null || true
		sleep 5

		echo ""
		echo "========================================="
		echo "  API 35 — Installing APKs"
		echo "========================================="
		set +e
		adb -s "$API35_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
		INSTALL_APP=$?
		adb -s "$API35_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
		INSTALL_TEST=$?
		set -e

		if [ $INSTALL_APP -ne 0 ] || [ $INSTALL_TEST -ne 0 ]; then
			echo "FAIL: APK installation failed (app=$INSTALL_APP, test=$INSTALL_TEST)"
			API35_RESULT=1
		else
			echo ""
			echo "========================================="
			echo "  API 35 — Running instrumented tests"
			echo "========================================="
			set +e
			adb -s "$API35_SERIAL" shell am instrument -w \
				at.priv.graf.zazentimer.test/at.priv.graf.zazentimer.HiltTestRunner
			API35_RESULT=$?
			set -e
		fi
	fi

	kill_emulator "$API35_SERIAL"
fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Stage 2 Summary"
echo "========================================="
echo "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
echo "API 29:     $([ $API29_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
echo "API 35:     $([ $API35_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
echo "========================================="

if [ $UNIT_RESULT -ne 0 ] || [ $API29_RESULT -ne 0 ] || [ $API35_RESULT -ne 0 ]; then
	exit 1
fi

exit 0
