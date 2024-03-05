#!/bin/bash
#
# Nextcloud Android client application
#
# @author Tobias Kaminsky
# Copyright (C) 2024 Tobias Kaminsky
# Copyright (C) 2024 Nextcloud GmbH
#  
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#  
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#  
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.
#

latestCommit=$(curl -s https://api.github.com/repos/nextcloud/android-library/commits/master | jq .sha | sed s'/\"//g')
currentCommit=$(grep "androidLibraryVersion" build.gradle | cut -f2 -d'"')

git fetch
git checkout master
git pull

[[ $latestCommit == "$currentCommit" ]] && exit # nothing to do

git fetch
git checkout -B update-library-"$(date +%F)" origin/master
 
sed -i s"#androidLibraryVersion\ =.*#androidLibraryVersion =\"$latestCommit\"#" build.gradle
./gradlew --console=plain --dependency-verification lenient -q --write-verification-metadata sha256,pgp help

git add build.gradle
git add gradle/verification-metadata.xml
git commit -s -m "Update library"
gh pr create --head "$(git branch --show-current)" --title "Update library $(date +%F)" --body "Update library to latest commit"
