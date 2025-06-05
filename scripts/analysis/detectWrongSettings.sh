#!/bin/bash

# SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../../"

snapshotCount=$("$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" dependencies | grep SNAPSHOT -c)
betaCount=$(grep "<bool name=\"is_beta\">true</bool>" "$PROJECT_ROOT/app/src/main/res/values/setup.xml" -c)
libraryHash=$(grep androidLibraryVersion "$PROJECT_ROOT/gradle/libs.versions.toml" | cut -d= -f2 | tr -d \"")

lastHashes=$(curl "https://api.github.com/repos/nextcloud/android-library/commits?sha=$baseBranch" | jq ".[] .sha" | head -n 20)
baseBranch="master"

if [[ $(echo "$lastHashes"  | grep -c $libraryHash) -ne 1  ]]; then
    echo "Library commit not within last 10 hashes, please rebase!"
    exit 1
fi

if [[ $snapshotCount -gt 0 ]] ; then
    echo "Snapshot found in dependencies"
    exit 1
fi
if [[ $betaCount -gt 0 ]] ; then
    echo "Beta is set in setup.xml"
    exit 1
fi
 
exit 0

