#!/usr/bin/env bash

counter=0
status=""

until [[ $status = "false" ]]; do
    status=$(curl 2>/dev/null "http://$1/status.php" | jq .maintenance)

    if [[ "$status" =~ "false" || "$status" = "" ]]; then
        let "counter += 1"
         if [[ $counter -gt 2 ]]; then
            echo "Failed to wait for server"
            exit 1
        fi
    fi

    sleep 10
done
