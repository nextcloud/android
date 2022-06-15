#!/bin/bash

count=$(./gradlew dependencies | grep SNAPSHOT | grep -v "com.github.nextcloud:android-library" -c)

if [ $count -eq 0 ] ; then
    exit 0
else
    exit 1
fi

