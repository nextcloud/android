#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2019-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2019-2020 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

counter=0
status=""

until [[ $status = "false" ]]; do
    status=$(curl 2>/dev/null "http://$1/status.php" | jq .maintenance)

    echo "($counter) $status"

    if [[ "$status" =~ "false" || "$status" = "" ]]; then
        let "counter += 1"
         if [[ $counter -gt 50 ]]; then
            echo "Failed to wait for server"
            exit 1
        fi
    fi

    sleep 10
done
