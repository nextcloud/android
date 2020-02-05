#!/usr/bin/env bash

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
REMOTE_FOLDER=$ID-$TYPE

set -e

if [ $TYPE = "IT" ]; then
    FOLDER=build/reports/androidTests/connected/flavors/GPLAY
elif [ $TYPE = "Unit" ]; then
    FOLDER=build/reports/tests/testGplayDebugUnitTest
else
    FOLDER=build/reports/shot/verification/
fi

if [ ! -e $FOLDER ]; then
    echo "$TYPE test failed, but no output was generated. Maybe a preliminary stage failed."

    curl -u $6:$7 \
    -X POST https://api.github.com/repos/nextcloud/android/issues/$5/comments \
    -d "{ \"body\" : \"$TYPE test failed, but no output was generated. Maybe a preliminary stage failed. \" }"

    exit 1
fi

cd $FOLDER

find . -type d -exec curl -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
find . -type f -exec curl -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

curl -u $6:$7 -X POST https://api.github.com/repos/nextcloud/android/issues/$5/comments \
-d "{ \"body\" : \"$TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER \" }"

exit 1
