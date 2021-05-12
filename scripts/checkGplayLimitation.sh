#!/usr/bin/env bash

result=""

for log in fastlane/metadata/android/*/changelogs/*
    do
    if [[ -e $log && $(wc -m $log | cut -d" " -f1) -gt 500 ]]
        then
        result=$log"<br>"$result
    fi
done

echo -e "$result";
