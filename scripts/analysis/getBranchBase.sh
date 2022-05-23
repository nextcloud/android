#!/bin/bash

GITHUB_TOKEN=$1
PR_NUMBER=$2

source scripts/lib.sh

if [ -z "${PR_NUMBER}" ] ; then
    echo "master";
else
    curl_gh "https://api.github.com/repos/nextcloud/android/pulls/${PR_NUMBER}" | jq .base.ref
fi
