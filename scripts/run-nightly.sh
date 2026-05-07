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
TODAY=$(date +%Y-%m-%d)
LOG_FILE=$PROJECT_DIR/logs/nightly-${TODAY}.log

XVFB_PID=""
UNIT_RESULT=0
declare -A RESULTS
FAILED_APIS=()
ERROR_LOGS=()

cleanup() {
	if [ -n "${XVFB_PID:-}" ]; then
		kill "$XVFB_PID" 2>/dev/null || true
		wait "$XVFB_PID" 2>/dev/null || true
	fi
	adb devices | grep -q "emulator" && adb -s emulator-5554 emu kill 2>/dev/null || true
}
trap cleanup EXIT

echo "=== Nightly Test Run — $TODAY ==="
echo "Logging to $LOG_FILE"

cd "$PROJECT_DIR"

if [ -n "$(git status --porcelain)" ]; then
	echo "ERROR: Git repository is not clean. Commit or stash changes before running."
	git status --short
	exit 1
fi

git fetch origin
git pull --ff-only origin main

if [ -z "${DISPLAY:-}" ]; then
	echo "=== Starting Xvfb on :99 ==="
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

	run_gradle_test() {
		local api_level=$1
		local serial="emulator-5554"
		local avd_name
		avd_name=$(resolve_avd "$api_level") || {
			echo "FAIL: Could not find AVD for API $api_level"
			RESULTS[$api_level]=1
			FAILED_APIS+=("$api_level")
			ERROR_LOGS+=("API $api_level: No AVD found")
			return
		}
		local result=0

		echo ""
		echo "========================================="
		echo "  API $api_level — Starting emulator ($avd_name)"
		echo "========================================="

		$ANDROID_HOME/emulator/emulator \
			-avd "$avd_name" \
			-no-snapshot \
			-gpu swiftshader_indirect \
			-noaudio \
			-no-boot-anim \
			-memory 4096 &
		sleep 2

		if ! wait_for_emulator "$serial"; then
			echo "FAIL: API $api_level emulator did not boot"
			RESULTS[$api_level]=1
			FAILED_APIS+=("$api_level")
			ERROR_LOGS+=("API $api_level: Emulator failed to boot")
			kill_emulator "$serial"
			return
		fi

		clean_device_packages "$serial"
		dismiss_anr_dialog "$serial"
		sleep 5

		echo ""
		echo "========================================="
		echo "  API $api_level — Running instrumented tests"
		echo "========================================="

		set +e
		cd "$PROJECT_DIR"
		./gradlew connectedDebugAndroidTest --no-daemon
		result=$?
		set -e

		if [ $result -ne 0 ]; then
			echo "FAIL: API $api_level tests failed (exit $result)"
			RESULTS[$api_level]=$result
			FAILED_APIS+=("$api_level")
			ERROR_LOGS+=("API $api_level: connectedDebugAndroidTest exit code $result")
		else
			echo "PASS: API $api_level"
			RESULTS[$api_level]=0
		fi

		kill_emulator "$serial"
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
			anr_window=$(adb -s "$serial" shell "dumpsys window windows" 2>/dev/null | grep -c "Application Error\|isn't responding\|is not responding")
			if [ "$anr_window" -eq 0 ]; then
				return 0
			fi
			echo "ANR dialog detected on $serial (attempt $attempt) — dismissing..."
			adb -s "$serial" shell input keyevent KEYCODE_DPAD_RIGHT 2>/dev/null
			adb -s "$serial" shell input keyevent KEYCODE_ENTER 2>/dev/null
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

	run_api35_instrument() {
		local api_level=35
		local serial="emulator-5554"
		local result=0

		echo ""
		echo "========================================="
		echo "  API 35 — Building APKs"
		echo "========================================="
		cd "$PROJECT_DIR"
		set +e
		./gradlew assembleDebug assembleDebugAndroidTest --no-daemon
		local build_result=$?
		set -e

		if [ $build_result -ne 0 ]; then
			echo "FAIL: API 35 build failed"
			RESULTS[35]=$build_result
			FAILED_APIS+=("35")
			ERROR_LOGS+=("API 35: Build failed (exit $build_result)")
			return
		fi

		local avd35
		avd35=$(resolve_avd 35) || {
			echo "FAIL: Could not find AVD for API 35"
			RESULTS[35]=1
			FAILED_APIS+=("35")
			ERROR_LOGS+=("API 35: No AVD found")
			return
		}

		echo ""
		echo "========================================="
		echo "  API 35 — Starting emulator ($avd35)"
		echo "========================================="

		$ANDROID_HOME/emulator/emulator \
			-avd "$avd35" \
			-no-snapshot \
			-gpu swiftshader_indirect \
			-noaudio \
			-no-boot-anim \
			-memory 4096 &
		sleep 2

		if ! wait_for_emulator "$serial"; then
			echo "FAIL: API 35 emulator did not boot"
			RESULTS[35]=1
			FAILED_APIS+=("35")
			ERROR_LOGS+=("API 35: Emulator failed to boot")
			kill_emulator "$serial"
			return
		fi

		clean_device_packages "$serial"
		dismiss_anr_dialog "$serial"
		sleep 5

		echo ""
		echo "========================================="
		echo "  API 35 — Installing APKs"
		echo "========================================="
		set +e
		adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
		local install_app=$?
		adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
		local install_test=$?
		set -e

		if [ $install_app -ne 0 ] || [ $install_test -ne 0 ]; then
			echo "FAIL: API 35 APK installation failed (app=$install_app, test=$install_test)"
			RESULTS[35]=1
			FAILED_APIS+=("35")
			ERROR_LOGS+=("API 35: APK install failed (app=$install_app, test=$install_test)")
			kill_emulator "$serial"
			return
		fi

		echo ""
		echo "========================================="
		echo "  API 35 — Running instrumented tests"
		echo "========================================="
		set +e
		adb -s "$serial" shell am instrument -w \
			at.priv.graf.zazentimer.test/at.priv.graf.zazentimer.HiltTestRunner
		result=$?
		set -e

		if [ $result -ne 0 ]; then
			echo "FAIL: API 35 am instrument failed (exit $result)"
			RESULTS[35]=$result
			FAILED_APIS+=("35")
			ERROR_LOGS+=("API 35: am instrument exit code $result")
		else
			echo "PASS: API 35"
			RESULTS[35]=0
		fi

		kill_emulator "$serial"
	}

	for api in 29 30 31 32 33 34; do
		run_gradle_test "$api"
	done

	run_api35_instrument
fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Nightly Test Summary — $TODAY"
echo "========================================="
echo "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
for api in 29 30 31 32 33 34 35; do
	status="PASS"
	if [ "${RESULTS[$api]:-1}" -ne 0 ]; then
		status="FAIL"
	fi
	echo "API $api:    $status"
done
echo "========================================="

if [ $UNIT_RESULT -ne 0 ] || [ ${#FAILED_APIS[@]} -gt 0 ]; then
	echo ""
	echo "=== Creating GitHub Issue for failure ==="

	cd "$PROJECT_DIR"

	existing_issue=$(gh issue list --state open --label "nightly-failure" --limit 1 --json number,title --jq '.[0]' 2>/dev/null || echo "")

	body=""
	body+="**Nightly test failure on $TODAY**\n\n"
	body+="## Failed\n\n"
	if [ $UNIT_RESULT -ne 0 ]; then
		body+="- Unit Tests: FAIL\n"
	fi
	for err in "${ERROR_LOGS[@]:-}"; do
		body+="- $err\n"
	done
	body+="\n## All Results\n\n"
	body+="- Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')\n"
	for api in 29 30 31 32 33 34 35; do
		status="PASS"
		if [ "${RESULTS[$api]:-1}" -ne 0 ]; then
			status="FAIL"
		fi
		body+="- API $api: $status\n"
	done
	body+="\n---\n_Auto-generated by nightly cron job_"

	if [ -n "$existing_issue" ] && [ "$existing_issue" != "null" ]; then
		issue_number=$(echo "$existing_issue" | jq -r '.number')
		echo "Found existing open nightly-failure issue #$issue_number — commenting"
		gh issue comment "$issue_number" --body "$(echo -e "**Update $TODAY**\n\n${body}")"
	else
		echo "Creating new nightly-failure issue"
		gh label create "nightly-failure" --color "FBCA04" --description "Automated nightly test failure" --force 2>/dev/null || true
		gh issue create \
			--title "Nightly test failure: $TODAY" \
			--body "$(echo -e "$body")" \
			--label "nightly-failure"
	fi

	exit 1
fi

echo ""
echo "All tests passed. No issue created."
exit 0
