#!/usr/bin/env bash

upload() {
    cd $1

    find . -type d -exec curl -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
    find . -type f -exec curl -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

    echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

    # delete all old comments, matching this type
    oldComments=$(curl 2>/dev/null -u $GITHUB_USER:$GITHUB_PASSWORD -X GET https://api.github.com/repos/nextcloud/android/issues/$PR/comments | jq --arg type $BRANCH_TYPE '.[] | (.id |tostring) + "|" + (.user.login | test("nextcloud-android-bot") | tostring) + "|" + (.body | test("[$TYPE] test failed.*") | tostring)'| grep "true|true" | tr -d "\"" | cut -f1 -d"|")

    echo $oldComments | while read comment ; do
        curl 2>/dev/null -u $GITHUB_USER:$GITHUB_PASSWORD -X DELETE https://api.github.com/repos/nextcloud/android/issues/comments/$comment
    done

    curl -u $GITHUB_USER:$GITHUB_PASSWORD -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER \" }"

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
BRANCH=$4
TYPE=$5
PR=$6
GITHUB_USER=$7
GITHUB_PASSWORD=$8
REMOTE_FOLDER=$ID-$TYPE-$BRANCH
BRANCH_TYPE=$BRANCH-$TYPE

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
    echo "$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed."

    curl -u $GITHUB_USER:$GITHUB_PASSWORD \
    -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed. \" }"

    if [ -e build/reports/androidTests/connected/flavors/GPLAY ] ; then
        TYPE="IT"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "build/reports/androidTests/connected/flavors/GPLAY"
    fi

    if [ -e build/reports/tests/testGplayDebugUnitTest ] ; then
        TYPE="Unit"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "build/reports/tests/testGplayDebugUnitTest"
    fi

    if [ -e build/reports/shot/verification ] ; then
        TYPE="Screenshot"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "build/reports/shot/verification"
    fi
fi
