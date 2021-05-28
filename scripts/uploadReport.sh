#!/usr/bin/env bash

deleteOldComments() {
    # delete all old comments, matching this type
    echo "Deleting old comments for $BRANCH_TYPE"
    oldComments=$(curl > /dev/null 2>&1 -u $GITHUB_USER:$GITHUB_PASSWORD -X GET https://api.github.com/repos/nextcloud/android/issues/$PR/comments | jq --arg TYPE $BRANCH_TYPE '.[] | (.id |tostring) + "|" + (.user.login | test("nextcloud-android-bot") | tostring) + "|" + (.body | test([$TYPE]) | tostring)'| grep "true|true" | tr -d "\"" | cut -f1 -d"|")
    count=$(echo $oldComments | grep true | wc -l)
    echo "Found $count old comments"

    echo $oldComments | while read comment ; do
        echo "Deleting comment: $comment"
        curl > /dev/null 2>&1 -u $GITHUB_USER:$GITHUB_PASSWORD -X DELETE https://api.github.com/repos/nextcloud/android/issues/comments/$comment
    done
}

upload() {
    deleteOldComments

    cd $1

    find . -type d -exec curl > /dev/null 2>&1 -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
    find . -type f -exec curl > /dev/null 2>&1 -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

    echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

    curl -u $GITHUB_USER:$GITHUB_PASSWORD -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER \" }"

    exit 1
}

#1: LOG_USERNAME
#2: LOG_PASSWORD
#3: DRONE_BUILD_NUMBER
#4: BRANCH (stable or master)
#5: TYPE (IT or Unit)
#6: DRONE_PULL_REQUEST
#7: GIT_USERNAME
#8: GIT_TOKEN

URL=https://nextcloud.kaminsky.me/remote.php/webdav/android-integrationTests
ID=$3
USER=$1
PASS=$2
BRANCH=$4
TYPE=$5
PR=$6
GITHUB_USER=$7
GITHUB_PASSWORD=$8
REMOTE_FOLDER=$ID-$TYPE-$BRANCH-$(date +%H-%M)
BRANCH_TYPE=$BRANCH-$TYPE

set -e

if [ -z $USER ] || [ -z $PASS ]; then
    echo "USER or PASS is empty!"
    exit 1
fi

if [ $TYPE = "IT" ]; then
    FOLDER=build/reports/androidTests/connected/flavors/gplay
elif [ $TYPE = "Unit" ]; then
    FOLDER=build/reports/tests/testGplayDebugUnitTest
else
    FOLDER=build/reports/shot/verification
fi

if [ -e $FOLDER ]; then
    upload $FOLDER
else
    deleteOldComments
    echo "$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed."

    curl > /dev/null 2>&1 -u $GITHUB_USER:$GITHUB_PASSWORD \
    -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed. \" }"

    if [ -e build/reports/androidTests/connected/flavors/gplayDebugAndroidTest ] ; then
        TYPE="IT"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "build/reports/androidTests/connected/flavors/gplayDebugAndroidTest"
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

    exit 1 # always fail
fi
