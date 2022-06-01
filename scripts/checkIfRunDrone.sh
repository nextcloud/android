#!/bin/sh -e

PR_NUMBER=$1

if [ -z "$PR_NUMBER" ] ; then
    echo "Merge commit to master -> continue with CI"
    exit 0
fi

export BRANCH=$(scripts/analysis/getBranchBase.sh "$PR_NUMBER" | sed 's/"//g')
if [ "$(git diff --name-only "origin/$BRANCH" | grep -cE "^app/src|screenshots|build.gradle|.drone.yml")" -eq 0 ] ; then
    echo "No source files changed"
    exit 1
else
    echo "Source files changed -> continue with CI"
    exit 0
fi
