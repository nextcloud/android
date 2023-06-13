#!/bin/bash

snapshotCount=$(./gradlew dependencies | grep SNAPSHOT | grep -v "com.github.nextcloud:android-library" -c)
betaCount=$(grep "<bool name=\"is_beta\">true</bool>" app/src/main/res/values/setup.xml -c)

if [[ $snapshotCount -eq 0 && $betaCount -eq 0 ]] ; then
    exit 0
else
    exit 1
fi

