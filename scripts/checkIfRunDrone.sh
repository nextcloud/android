#!/bin/sh -e

# SPDX-FileCopyrightText: 2019-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2019-2022 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

PR_NUMBER=$1

if [ -z "$PR_NUMBER" ] ; then
    echo "Merge commit to master -> continue with CI"
    exit 0
fi

export BRANCH=$(scripts/analysis/getBranchBase.sh "$PR_NUMBER" | sed 's/"//g')
if [ "$(git diff --name-only "origin/$BRANCH" | grep -cE "^app/src|screenshots|build.gradle.kts|.drone.yml|gradle")" -eq 0 ] ; then
    echo "No source files changed"
    exit 1
else
    echo "Source files changed -> continue with CI"
    exit 0
fi
