#!/bin/sh

# $1: username, $2: password/token, $3: url

curl 2>/dev/null -u $1:$2 $3 | grep \"ref\": | grep -v master | cut -d"\"" -f4