#!/usr/bin/env bash
#
# SPDX-FileCopyrightText: 2022-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
# SPDX-License-Identifier: AGPL-3.0-or-later
#

## This file is intended to be sourced by other scripts


function err() {
    echo >&2 "$@"
}


function curl_gh() {
    if [[ -n "$GITHUB_TOKEN" ]]
    then
        curl \
            --silent \
            --header "Authorization: token $GITHUB_TOKEN" \
            "$@"
    else
        err "WARNING: No GITHUB_TOKEN found. Skipping API call"
    fi

}
