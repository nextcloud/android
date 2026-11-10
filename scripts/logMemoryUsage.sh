#!/bin/bash

# SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
# SPDX-License-Identifier: AGPL-3.0-or-later

INTERVAL_SECONDS=15
LOW_MEMORY_MB=1024
TOP_PROCESS_COUNT=5
KIB_PER_MIB=1024

low_memory_reported=false
reported_oom_lines=0

function process_label() {
    case "$1" in
        *KotlinCompileDaemon*) echo "kotlin-daemon" ;;
        *GradleDaemon*) echo "gradle-daemon" ;;
        *GradleWrapperMain*) echo "gradle-client" ;;
        *qemu-system*) echo "emulator" ;;
        *) basename "${1%% *}" ;;
    esac
}

function top_processes() {
    ps -eo rss=,args= --sort=-rss | head -n "$TOP_PROCESS_COUNT" | while read -r rss args; do
        printf "%s=%dM " "$(process_label "$args")" $((rss / KIB_PER_MIB))
    done
}

function report_oom_kills() {
    local oom_lines
    oom_lines=$(sudo -n dmesg 2>/dev/null | grep -E "Out of memory|oom-kill|Killed process")
    local count
    count=$(echo -n "$oom_lines" | grep -c '^')
    if [[ $count -le $reported_oom_lines ]]; then
        return
    fi
    echo "$oom_lines" | tail -n $((count - reported_oom_lines)) | while read -r line; do
        echo "::error title=Out of memory::$line"
    done
    reported_oom_lines=$count
}

function report_low_memory() {
    local available_mb=$1
    local summary=$2
    if [[ $available_mb -ge $LOW_MEMORY_MB ]]; then
        low_memory_reported=false
        return
    fi
    if [[ $low_memory_reported == true ]]; then
        return
    fi
    echo "::warning title=Low memory::$summary"
    low_memory_reported=true
}

while true; do
    read -r total_mb used_mb available_mb < <(free -m | awk '/^Mem:/ {print $2, $3, $7}')
    swap=$(free -m | awk '/^Swap:/ {print $3 "/" $2}')
    summary="used=${used_mb}M available=${available_mb}M/${total_mb}M swap=${swap}M top: $(top_processes)"
    echo "[memory] $summary"
    report_low_memory "$available_mb" "$summary"
    report_oom_kills
    sleep "$INTERVAL_SECONDS"
done
