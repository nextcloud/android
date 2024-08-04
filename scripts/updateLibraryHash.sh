#!/bin/bash
#
# SPDX-FileCopyrightText: 2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

git fetch
git checkout master
git pull

latestCommit=$(curl -s https://api.github.com/repos/nextcloud/android-library/commits/master | jq .sha | sed s'/\"//g')
currentCommit=$(grep "androidLibraryVersion" build.gradle | cut -f2 -d'"')

[[ $latestCommit == "$currentCommit" ]] && echo "Nothing to do. Commit is: $latestCommit" && exit # nothing to do

git fetch
git checkout -B update-library-"$(date +%F)" origin/master
 
sed -i s"#androidLibraryVersion\ =.*#androidLibraryVersion =\"$latestCommit\"#" build.gradle
./gradlew --console=plain --dependency-verification lenient -q --write-verification-metadata sha256,pgp help

git add build.gradle
git add gradle/verification-metadata.xml
git commit -s -m "Update library to $(date +%F)" 

git push -u origin HEAD
gh pr create --fill
