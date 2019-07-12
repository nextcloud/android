#!/bin/sh

export BRANCH=$(scripts/analysis/getBranchName.sh $GIT_USERNAME $GIT_TOKEN $DRONE_PULL_REQUEST)
[ $(git diff --name-only origin/$BRANCH | grep -c "^src") -eq 0 ] && echo "No source files changed" && exit 1
