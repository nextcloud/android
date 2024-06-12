#!/bin/bash

# SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

snapshotCount=$(./gradlew dependencies | grep SNAPSHOT -c)
betaCount=$(grep "<bool name=\"is_beta\">true</bool>" app/src/main/res/values/setup.xml -c)
libraryHash=$(grep androidLibraryVersion build.gradle | cut -f2 -d'"' | grep "^[0-9a-zA-Z]\{40\}$" -c)


if [[ $snapshotCount -gt 0 ]] ; then
    echo "Snapshot found in dependencies"
    exit 1
fi
if [[ $betaCount -gt 0 ]] ; then
    echo "Beta is set in setup.xml"
    exit 1
fi
 
if [[ $libraryHash -eq 0 ]] ; then
    echo "Library hash is wrong!"
    exit 1
fi

exit 0

