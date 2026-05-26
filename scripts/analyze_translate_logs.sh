#!/bin/bash
# Analyze translate pipeline logs: per (model, locale) duration + ms/string
# Usage: scripts/analyze_translate_logs.sh [--file <path>] [--model <name>]
#   --file     Path to orchestrator log (default: logs/orchestrator.log)
#   --model    Filter to specific model (e.g. "glm-5.1")

FILE="logs/orchestrator.log"
MODEL_FILTER=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --file) FILE="$2"; shift 2 ;;
    --model) MODEL_FILTER="$2"; shift 2 ;;
    *) echo "Usage: $0 [--file <path>] [--model <name>]"; exit 1 ;;
  esac
done

if [[ ! -f "$FILE" ]]; then
  echo "Log file not found: $FILE"
  exit 1
fi

# Parse submitting lines: extract timestamp, locale, provider, model
# Format: [2026-05-26_10-00-00] submitting translation request for Polish to zai-coding-plan(rank 0)/glm-5.1, ...
awk '
function to_sec(t) {
    # Format: 2026-05-26_10-02-44
    split(t, parts, /[-_]/)
    # parts[1]=year, parts[2]=month, parts[3]=day, parts[4]=hour, parts[5]=minute, parts[6]=second
    return parts[4] * 3600 + parts[5] * 60 + parts[6]
}

function fmt_duration(sec) {
    m = int(sec / 60)
    s = int(sec % 60)
    return sprintf("%d:%02d", m, s)
}

/\] submitting translation request for / {
    ts = substr($1, 2, 19)  # "2026-05-26_10-00-00"
    # Find locale: after "for " up to " to "
    line = $0
    start = index(line, " for ") + 5
    end = index(line, " to ")
    locale = substr(line, start, end - start)

    # Find provider/model: between "to " and ", proficiency"
    prov_start = end + 4
    prov_end = index(line, ", proficiency")
    prov_model = substr(line, prov_start, prov_end - prov_start)

    # Split "provider(rank N)/model"
    split(prov_model, a, "/")
    provider_rank = a[1]
    model = a[2]

    # store by (model, locale) key
    key = model SUBSEP locale
    submit_ts[key] = ts
    submit_provider[key] = provider_rank
    submit_model[key] = model
    submit_locale[key] = locale
}

/\] got translation result for / {
    ts = substr($1, 2, 19)
    line = $0
    start = index(line, " for ") + 5
    end = index(line, " to ")
    locale = substr(line, start, end - start)

    prov_start = end + 4
    prov_end = index(line, ": stored")
    prov_model = substr(line, prov_start, prov_end - prov_start)
    split(prov_model, a, "/")
    model = a[2]

    # Extract stored and skipped
    line_rest = substr($0, index($0, ": stored"))
    split(line_rest, parts, /[,:]/)
    gsub(/[^0-9]/, "", parts[2]); stored = parts[2] + 0
    gsub(/[^0-9]/, "", parts[3]); skipped = parts[3] + 0

    key = model SUBSEP locale
    result_ts[key] = ts
    result_stored[key] = stored
    result_skipped[key] = skipped
}

END {
    printf("%-16s %-16s %-10s %6s %7s %10s\n", "Model", "Locale", "Duration", "Stored", "Skipped", "ms/string")
    printf("%-16s %-16s %-10s %6s %7s %10s\n", "---", "---", "---", "---", "---", "---")

    # Track per-model totals for summary
    delete model_total_time
    delete model_total_strings
    delete model_count

    # Process keys in order of submission
    n = asorti(submit_ts, sorted)
    for (i = 1; i <= n; i++) {
        key = sorted[i]
        if (!(key in result_ts)) continue

        model = submit_model[key]
        locale = submit_locale[key]
        prov = submit_provider[key]

        t1 = to_sec(submit_ts[key])
        t2 = to_sec(result_ts[key])
        if (t2 < t1) t2 += 86400  # across midnight
        dur_sec = t2 - t1

        stored = result_stored[key] + 0
        skipped = result_skipped[key] + 0

        if (stored > 0) {
            ms_per = int((dur_sec * 1000) / stored)
        } else {
            ms_per = "-"
        }

        printf("%-16s %-16s %-10s %6d %7d %10s\n",
            model, locale, fmt_duration(dur_sec), stored, skipped,
            ms_per == "-" ? "∞" : sprintf("%d", ms_per))

        model_total_time[model] += dur_sec
        model_total_strings[model] += stored
        model_count[model]++
    }

    # Summary
    printf("\n%-16s %-16s %-10s %6s %7s %10s\n", "Model (avg)", "Runs", "∅ Duration", "∅ Stored", "", "∅ ms/string")
    printf("%-16s %-16s %-10s %6s %7s %10s\n", "---", "---", "---", "---", "---", "---")
    for (m in model_count) {
        avg_dur = int(model_total_time[m] / model_count[m])
        avg_str = int(model_total_strings[m] / model_count[m])
        if (model_total_strings[m] > 0) {
            avg_ms = int((model_total_time[m] * 1000) / model_total_strings[m])
        } else {
            avg_ms = "-"
        }
        printf("%-16s %-16d %-10s %6d %7s %10s\n",
            m, model_count[m], fmt_duration(avg_dur), avg_str, "",
            avg_ms == "-" ? "∞" : sprintf("%d", avg_ms))
    }
}
' "$FILE"
