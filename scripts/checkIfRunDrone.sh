#!/bin/sh -e

if [ -z $3 ] ; then
    echo "Merge commit to master -> continue with CI"
    exit 0
fi

export BRANCH=$(scripts/analysis/getBranchBase.sh $1 $2 $3 | sed s'/"//'g)
if [ $(git diff --name-only origin/$BRANCH | grep -cE "^src|build.gradle") -eq 0 ] ; then
    echo "No source files changed"
    exit 1
else
    echo "Source files changed -> continue with CI"
    exit 0
fi
