#!/usr/bin/env bash

curl_gh() {
  curl \
    --header "authorization: Bearer $GITHUB_TOKEN" \
    "$@"
}

deleteOldComments() {
    # delete all old comments, matching this type
    echo "Deleting old comments for $BRANCH_TYPE"
    oldComments=$(curl_gh > /dev/null 2>&1  -X GET https://api.github.com/repos/nextcloud/android/issues/$PR/comments | jq --arg TYPE $BRANCH_TYPE '.[] | (.id |tostring) + "|" + (.user.login | test("nextcloud-android-bot") | tostring) + "|" + (.body | test([$TYPE]) | tostring)'| grep "true|true" | tr -d "\"" | cut -f1 -d"|")
    count=$(echo $oldComments | grep true | wc -l)
    echo "Found $count old comments"

    echo $oldComments | while read comment ; do
        echo "Deleting comment: $comment"
        curl_gh > /dev/null 2>&1 -X DELETE https://api.github.com/repos/nextcloud/android/issues/comments/$comment
    done
}

upload() {
    deleteOldComments

    cd $1

    find . -type d -exec curl > /dev/null 2>&1 -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
    find . -type f -exec curl > /dev/null 2>&1 -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

    echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

    curl_gh -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER \" }"

    exit 1
}

#1: LOG_USERNAME
#2: LOG_PASSWORD
#3: DRONE_BUILD_NUMBER
#4: BRANCH (stable or master)
#5: TYPE (IT or Unit)
#6: DRONE_PULL_REQUEST
#7: GITHUB_TOKEN

URL=https://nextcloud.kaminsky.me/remote.php/webdav/android-integrationTests
ID=$3
USER=$1
PASS=$2
BRANCH=$4
TYPE=$5
PR=$6
GITHUB_TOKEN="$7"

REMOTE_FOLDER=$ID-$TYPE-$BRANCH-$(date +%H-%M)
BRANCH_TYPE=$BRANCH-$TYPE

set -e

if [ -z $USER ] || [ -z $PASS ]; then
    echo "USER or PASS is empty!"
    exit 1
fi

if [ $TYPE = "IT" ]; then
    FOLDER=app/build/reports/androidTests/connected/flavors/gplay
elif [ $TYPE = "Unit" ]; then
    FOLDER=app/build/reports/tests/testGplayDebugUnitTest
else
    FOLDER=app/build/reports/shot/gplay/debug/verification
fi

if [ -e $FOLDER ]; then
    upload $FOLDER
else
    deleteOldComments
    echo "$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed."

    curl_gh > /dev/null 2>&1  \
    -X POST https://api.github.com/repos/nextcloud/android/issues/$PR/comments \
    -d "{ \"body\" : \"$BRANCH_TYPE test failed, but no output was generated. Maybe a preliminary stage failed. \" }"

    if [ -e app/build/reports/androidTests/connected/flavors/gplay ] ; then
        TYPE="IT"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "app/build/reports/androidTests/connected/flavors/gplay"
    fi

    if [ -e app/build/reports/tests/testGplayDebugUnitTest ] ; then
        TYPE="Unit"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "app/build/reports/tests/testGplayDebugUnitTest"
    fi

    if [ -e app/build/reports/shot/gplay/debug/verification ] ; then
        TYPE="Screenshot"
        BRANCH_TYPE=$BRANCH-$TYPE
        upload "app/build/reports/shot/gplay/debug/verification"
    fi

    exit 1 # always fail
fi
