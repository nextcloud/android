#!/bin/bash

# SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

PR_NUMBER=$1

source scripts/lib.sh

if [ -z "${PR_NUMBER}" ] ; then
    echo "master";
else
    curl_gh "https://api.github.com/repos/nextcloud/android/pulls/${PR_NUMBER}" | jq .base.ref
fi
