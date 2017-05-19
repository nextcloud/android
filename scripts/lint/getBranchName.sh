#!/bin/bash

# $1: username, $2: password/token, $3: pull request number

curl 2>/dev/null -u $1:$2 https://api.github.com/repos/nextcloud/android/pulls/$3 | grep \"ref\": | grep -v master | cut -d"\"" -f4