#!/bin/bash

# SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

# $1: username, $2: password/token, $3: pull request number

if [ -z $3 ] ; then
    echo "master";
else
    curl 2>/dev/null -u $1:$2 https://api.github.com/repos/nextcloud/android/pulls/$3 | grep \"ref\": | grep -v '"master"' | cut -d"\"" -f4
fi
