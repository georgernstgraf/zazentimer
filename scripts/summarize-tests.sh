#!/bin/bash
set -uo pipefail

DATE=""
MARKDOWN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
    --date)
        DATE="$2"
        shift 2
        ;;
    --markdown)
        MARKDOWN=true
        shift
        ;;
    *)
        echo "Usage: $0 [--date YYYY-MM-DD] [--markdown]"
        exit 2
        ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -z "$DATE" ]; then
    DATE=$(date +%Y-%m-%d)
fi

MAIN_LOG="$PROJECT_DIR/logs/instrumentation-${DATE}.log"

if [ ! -f "$MAIN_LOG" ]; then
    echo "ERROR: Main log not found: $MAIN_LOG"
    exit 2
fi

OUTPUT=""

out() {
    OUTPUT+="$*"$'\n'
}

separator() {
    out ""
}

api_logs=()
while IFS= read -r -d '' f; do
    api_logs+=("$f")
done < <(find "$PROJECT_DIR/logs" -maxdepth 1 -name "api*-${DATE}.log" -print0 2>/dev/null | sort -z)

logcat_files=()
while IFS= read -r -d '' f; do
    logcat_files+=("$f")
done < <(find "$PROJECT_DIR/logs" -maxdepth 1 -name "api*-${DATE}-logcat.txt" -print0 2>/dev/null | sort -z)

crashdb_dirs=()
while IFS= read -r -d '' d; do
    crashdb_dirs+=("$d")
done < <(find "$PROJECT_DIR/logs" -maxdepth 1 -type d -name "crashdb-api*-${DATE}" -print0 2>/dev/null | sort -z)

unit_xml_dir="$PROJECT_DIR/app/build/test-results/testDebugUnitTest"
instrumented_xml_dir="$PROJECT_DIR/app/build/outputs/androidTest-results/connected/debug"

# ──────────────────────────────────────────────
# Parse main log
# ──────────────────────────────────────────────

first_ts=$(grep -oP '^\[\K[0-9]{2}:[0-9]{2}:[0-9]{2}' "$MAIN_LOG" | head -1)
last_ts=$(grep -oP '^\[\K[0-9]{2}:[0-9]{2}:[0-9]{2}' "$MAIN_LOG" | tail -1)

run_start_line=$(grep -n "=== Instrumentation Test Run" "$MAIN_LOG" | head -1 | cut -d: -f1)
run_end_line=$(wc -l < "$MAIN_LOG")

display_mode="unknown"
if grep -q "using existing display" "$MAIN_LOG"; then
    display_mode="real"
elif grep -q "Starting Xvfb" "$MAIN_LOG"; then
    display_mode="Xvfb"
fi

unit_result="UNKNOWN"
if grep -q "PASS: Unit tests" "$MAIN_LOG"; then
    unit_result="PASS"
elif grep -q "FAIL: Unit tests" "$MAIN_LOG"; then
    unit_result="FAIL"
fi

auto_tag=""
if grep -q "tagging HEAD as tested-" "$MAIN_LOG"; then
    auto_tag=$(grep -oP 'tagging HEAD as \K\S+' "$MAIN_LOG")
fi

declare -A API_RESULT
declare -A API_SUMMARY_LINE
while IFS= read -r line; do
    api=$(echo "$line" | grep -oP 'API\s+\K[0-9]+')
    result=$(echo "$line" | grep -oP 'API\s+[0-9]+:\s+\K(PASS|FAIL|SKIPPED)')
    if [ -n "$api" ] && [ -n "$result" ]; then
        API_RESULT[$api]=$result
        API_SUMMARY_LINE[$api]=$line
    fi
done < <(grep -E "API\s+[0-9]+:\s+(PASS|FAIL|SKIPPED)" "$MAIN_LOG")

api_order=()
for f in "${api_logs[@]}"; do
    api=$(basename "$f" | grep -oP 'api\K[0-9]+')
    [ -n "$api" ] && api_order+=("$api")
done

if [ ${#api_order[@]} -eq 0 ]; then
    for api in $(echo "${!API_RESULT[@]}" | tr ' ' '\n' | sort -rn); do
        api_order+=("$api")
    done
fi

# ──────────────────────────────────────────────
# Parse per-API logs
# ──────────────────────────────────────────────

declare -A API_BUILD_TIME
declare -A API_BUILD_RESULT
declare -A API_TEST_PROGRESS
declare -A API_TEST_FAILED_COUNT
declare -A API_FAILED_TESTS
declare -A API_ERROR_PATTERNS
declare -A API_LOG_FILE

for f in "${api_logs[@]}"; do
    api=$(basename "$f" | grep -oP 'api\K[0-9]+')
    [ -z "$api" ] && continue
    API_LOG_FILE[$api]=$f

    bt=$(grep -oP 'BUILD (SUCCESSFUL|FAILED) in \K.*' "$f" | tail -1)
    if [ -n "$bt" ]; then
        API_BUILD_TIME[$api]=$bt
        br=$(echo "$bt" | grep -q "FAILED" && echo "FAIL" || echo "PASS")
        bt_only=$(echo "$bt" | grep -oP '[0-9]+m [0-9]+s')
        API_BUILD_TIME[$api]=${bt_only:-$bt}
    fi

    if grep -q "BUILD FAILED" "$f"; then
        API_BUILD_RESULT[$api]="FAIL"
    elif grep -q "BUILD SUCCESSFUL" "$f"; then
        API_BUILD_RESULT[$api]="PASS"
    fi

    progress=$(grep -oP 'Tests \K[0-9]+/[0-9]+ completed' "$f" | tail -1)
    [ -n "$progress" ] && API_TEST_PROGRESS[$api]=$progress

    failed_count=$(grep -oP '\(([0-9]+) failed\)' "$f" | tail -1 | grep -oP '[0-9]+')
    [ -n "$failed_count" ] && API_TEST_FAILED_COUNT[$api]=$failed_count

    failed_tests=""
    while IFS= read -r ft; do
        test_name=$(echo "$ft" | sed 's/\x1b\[[0-9;]*m//g' | grep -oP '[\w.]+Test > \K[\w]+' || true)
        if [ -n "$test_name" ]; then
            [ -n "$failed_tests" ] && failed_tests+=", "
            failed_tests+="$test_name"
        fi
    done < <(grep "FAILED" "$f" | grep -v "BUILD FAILED" | grep -v "Execute " | grep -v "FAIL:" || true)
    [ -n "$failed_tests" ] && API_FAILED_TESTS[$api]=$failed_tests

    errors=""
    if grep -q "keyDispatchingTimedOut" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="keyDispatchingTimedOut"
    fi
    if grep -q "Process crashed" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="Process crashed"
    fi
    if grep -q "RootViewWithoutFocusException" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="RootViewWithoutFocus"
    fi
    if grep -q "NoMatchingViewException" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="NoMatchingView"
    fi
    if grep -q "UTP was aborted" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="UTP aborted"
    fi
    if grep -q "Instrumentation run failed" "$f"; then
        [ -n "$errors" ] && errors+=", "
        errors+="Instrumentation failed"
    fi
    [ -n "$errors" ] && API_ERROR_PATTERNS[$api]=$errors
done

# ──────────────────────────────────────────────
# Parse JUnit XML (unit tests)
# ──────────────────────────────────────────────

declare -A UNIT_SUITE_RESULTS
unit_total_tests=0
unit_total_failures=0
unit_total_errors=0

if [ -d "$unit_xml_dir" ]; then
    for xml in "$unit_xml_dir"/TEST-*.xml; do
        [ -f "$xml" ] || continue
        suite_name=$(grep -oP 'name="\K[^"]+' "$xml" | head -1)
        tests=$(grep -oP 'tests="\K[0-9]+' "$xml" | head -1)
        failures=$(grep -oP 'failures="\K[0-9]+' "$xml" | head -1)
        errors=$(grep -oP 'errors="\K[0-9]+' "$xml" | head -1)
        time=$(grep -oP 'time="\K[^"]+' "$xml" | head -1)
        [ -z "$tests" ] && tests=0
        [ -z "$failures" ] && failures=0
        [ -z "$errors" ] && errors=0
        unit_total_tests=$((unit_total_tests + tests))
        unit_total_failures=$((unit_total_failures + failures))
        unit_total_errors=$((unit_total_errors + errors))
        UNIT_SUITE_RESULTS[$suite_name]="$tests $failures $errors $time"
    done
fi

# ──────────────────────────────────────────────
# Parse JUnit XML (instrumented tests)
# ──────────────────────────────────────────────

declare -A INST_DEVICE
declare -A INST_SUITE_TESTS
declare -A INST_SUITE_FAILURES
declare -A INST_SUITE_TIME
declare -A INST_FAILED_TESTCASES

if [ -d "$instrumented_xml_dir" ]; then
    for xml in "$instrumented_xml_dir"/TEST-*.xml; do
        [ -f "$xml" ] || continue
        device=$(grep -oP 'device" value="\K[^"]+' "$xml" | head -1)

        while IFS= read -r suite_block; do
            sname=$(echo "$suite_block" | grep -oP 'name="\K[^"]+' | head -1)
            stests=$(echo "$suite_block" | grep -oP 'tests="\K[0-9]+' | head -1)
            sfails=$(echo "$suite_block" | grep -oP 'failures="\K[0-9]+' | head -1)
            stime=$(echo "$suite_block" | grep -oP 'time="\K[^"]+' | head -1)
            [ -z "$stests" ] && stests=0
            [ -z "$sfails" ] && sfails=0
            key="${device:-unknown}|${sname}"
            INST_DEVICE[$key]=${device:-unknown}
            INST_SUITE_TESTS[$key]=$stests
            INST_SUITE_FAILURES[$key]=$sfails
            INST_SUITE_TIME[$key]=${stime:-0}

            while IFS= read -r tc_block; do
                tc_name=$(echo "$tc_block" | grep -oP 'name="\K[^"]+' | head -1)
                tc_class=$(echo "$tc_block" | grep -oP 'classname="\K[^"]+' | head -1)
                tc_time=$(echo "$tc_block" | grep -oP 'time="\K[^"]+' | head -1)
                fail_msg=""
                if echo "$tc_block" | grep -q "<failure"; then
                    fail_msg=$(echo "$tc_block" | grep -oP '<failure[^>]*>\K[^<]*' | head -1)
                    [ -z "$fail_msg" ] && fail_msg="(no message)"
                fi
                if [ -n "$fail_msg" ]; then
                    tc_key="${device:-unknown}"
                    existing="${INST_FAILED_TESTCASES[$tc_key]:-}"
                    entry="${tc_class}#${tc_name} (${tc_time}s): ${fail_msg}"
                    if [ -n "$existing" ]; then
                        INST_FAILED_TESTCASES[$tc_key]="${existing}
${entry}"
                    else
                        INST_FAILED_TESTCASES[$tc_key]="$entry"
                    fi
                fi
            done < <(echo "$suite_block" | grep -oP '<testcase.*?</testcase>' || true)
        done < <(grep -oP '<testsuite.*?</testsuite>' "$xml" || true)
    done
fi

# ──────────────────────────────────────────────
# Build output
# ──────────────────────────────────────────────

out "# Test Report — $DATE"
separator

out "| Field | Value |"
out "|-------|-------|"
out "| Date | $DATE |"
out "| Duration | ${first_ts:-?} → ${last_ts:-?} |"
out "| Display | $display_mode |"
out "| Unit Tests | $unit_result |"
if [ -n "$auto_tag" ]; then
    out "| Auto-tag | $auto_tag |"
fi
out "| API levels tested | ${#api_order[@]} |"
separator

out "## Summary"
separator

out "| API | Result | Tests | Build Time | Failed Tests | Error Patterns |"
out "|-----|--------|-------|------------|--------------|----------------|"

any_fail=0
for api in "${api_order[@]}"; do
    result="${API_RESULT[$api]:-UNKNOWN}"
    progress="${API_TEST_PROGRESS[$api]:---}"
    bt="${API_BUILD_TIME[$api]:---}"
    ft="${API_FAILED_TESTS[$api]:-—}"
    ep="${API_ERROR_PATTERNS[$api]:-—}"

    if [ "$result" = "FAIL" ] || [ "$result" = "UNKNOWN" ]; then
        any_fail=1
    fi

    out "| $api | $result | $progress | $bt | $ft | $ep |"
done

if [ ${#api_order[@]} -eq 0 ]; then
    out "| — | No API results found in log | — | — | — | — |"
fi

separator

out "## Unit Test Details"
separator

if [ ${#UNIT_SUITE_RESULTS[@]} -gt 0 ]; then
    out "| Suite | Tests | Failures | Errors | Time (s) |"
    out "|-------|-------|----------|--------|----------|"
    for suite in $(echo "${!UNIT_SUITE_RESULTS[@]}" | tr ' ' '\n' | sort); do
        read -r t f e tm <<< "${UNIT_SUITE_RESULTS[$suite]}"
        short_name=$(echo "$suite" | sed 's/at\.priv\.graf\.zazentimer\.//')
        out "| $short_name | $t | $f | $e | $tm |"
    done
    out "| **Total** | **$unit_total_tests** | **$unit_total_failures** | **$unit_total_errors** | |"
else
    out "_No unit test XML results found at $unit_xml_dir_"
fi

separator

has_failures=false
for api in "${api_order[@]}"; do
    result="${API_RESULT[$api]:-UNKNOWN}"
    if [ "$result" = "FAIL" ] || [ "$result" = "UNKNOWN" ]; then
        has_failures=true
        log_file="${API_LOG_FILE[$api]:-}"

        out "## API $api — Failure Details"
        separator

        if [ -n "$log_file" ] && [ -f "$log_file" ]; then
            logcat_file="$PROJECT_DIR/logs/api${api}-${DATE}-logcat.txt"
            logcat_info="—"
            if [ -f "$logcat_file" ]; then
                logcat_size=$(du -h "$logcat_file" | cut -f1)
                logcat_info="${logcat_size}"
            fi

            out "- **Log**: \`$(basename "$log_file")\`"
            out "- **Logcat**: \`$(basename "$logcat_file" 2>/dev/null || echo "none")\` ($logcat_info)"

            if [ -n "${API_FAILED_TESTS[$api]:-}" ]; then
                out "- **Failed tests**: ${API_FAILED_TESTS[$api]}"
            fi
            if [ -n "${API_ERROR_PATTERNS[$api]:-}" ]; then
                out "- **Error patterns**: ${API_ERROR_PATTERNS[$api]}"
            fi

            grep -A2 "FAILED" "$log_file" 2>/dev/null | grep -v "BUILD FAILED" | grep -v "^--$" | head -20 | while IFS= read -r fl; do
                trimmed=$(echo "$fl" | sed 's/\x1b\[[0-9;]*m//g' | xargs)
                [ -n "$trimmed" ] && out "  - \`$trimmed\`"
            done || true
        else
            out "_No per-API log found_"
        fi
        separator
    fi
done

if [ "$has_failures" = false ]; then
    out "## Failure Details"
    separator
    out "_No failures detected._"
    separator
fi

if [ ${#logcat_files[@]} -gt 0 ]; then
    out "## Logcat Files"
    separator
    for f in "${logcat_files[@]}"; do
        size=$(du -h "$f" | cut -f1)
        lines=$(wc -l < "$f")
        out "- \`$(basename "$f")\` — $size ($lines lines)"
    done
    separator
fi

if [ ${#crashdb_dirs[@]} -gt 0 ]; then
    out "## Crash DBs"
    separator
    for d in "${crashdb_dirs[@]}"; do
        count=$(find "$d" -type f | wc -l)
        size=$(du -sh "$d" | cut -f1)
        out "- \`$(basename "$d")/\` — $count files, $size"
    done
    separator
fi

out "---"
out "_Generated by scripts/summarize-tests.sh_"

# ──────────────────────────────────────────────
# Output
# ──────────────────────────────────────────────

if [ "$MARKDOWN" = true ]; then
    report_file="$PROJECT_DIR/logs/test-report-${DATE}.md"
    echo "$OUTPUT" > "$report_file"
    echo "Report written to $report_file"
else
    echo "$OUTPUT"
fi

if [ "$any_fail" -eq 1 ]; then
    exit 1
fi
exit 0
