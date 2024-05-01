#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2019-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
#

result=""

for log in fastlane/metadata/android/*/changelogs/*
    do
    if [[ -e $log && $(wc -m $log | cut -d" " -f1) -gt 500 ]]
        then
        result=$log"<br>"$result
    fi
done

echo -e "$result";
