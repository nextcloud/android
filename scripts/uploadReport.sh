#!/usr/bin/env bash

upload() {
    cd $1

    find . -type d -exec curl -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
    find . -type f -exec curl -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

    echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

    curl -u $GITHUB_USER:$GITHUB_PASSWORD -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER \" }"

    exit 1
}

#1: LOG_USERNAME
#2: LOG_PASSWORD
#3: DRONE_BUILD_NUMBER
#4: TYPE (IT or Unit)
#5: DRONE_PULL_REQUEST
#6: GIT_USERNAME
#7: GIT_TOKEN

URL=https://nextcloud.kaminsky.me/remote.php/webdav/android-integrationTests
ID=$3
USER=$1
PASS=$2
TYPE=$4
PR=$5
GITHUB_USER=$6
GITHUB_PASSWORD=$7
REMOTE_FOLDER=$ID-$TYPE

set -e

if [ $TYPE = "IT" ]; then
    FOLDER=build/reports/androidTests/connected/flavors/GPLAY
elif [ $TYPE = "Unit" ]; then
    FOLDER=build/reports/tests/testGplayDebugUnitTest
else
    FOLDER=build/reports/shot/verification
fi

if [ -e $FOLDER ]; then
    upload $FOLDER
else
    echo "$TYPE test failed, but no output was generated. Maybe a preliminary stage failed."

    curl -u $GITHUB_USER:$GITHUB_PASSWORD \
    -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$TYPE test failed, but no output was generated. Maybe a preliminary stage failed. \" }"

    if [ -e build/reports/androidTests/connected/flavors/GPLAY ] ; then
        TYPE="IT"
        upload "build/reports/androidTests/connected/flavors/GPLAY"
    fi

    if [ -e build/reports/tests/testGplayDebugUnitTest ] ; then
        TYPE="Unit"
        upload "build/reports/tests/testGplayDebugUnitTest"
    fi

    if [ -e build/reports/shot/verification ] ; then
        TYPE="Screenshot"
        upload "build/reports/shot/verification"
    fi
fi
