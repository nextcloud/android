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
FOLDER=$ID-$TYPE

set -e

if [ $TYPE = "IT" ]; then
    cd build/reports/androidTests/connected/flavors/GPLAY
elif [ $TYPE = "Unit" ]; then
    cd build/reports/tests/testGplayDebugUnitTest
else
    cd build/reports/shot/verification/
fi

find . -type d -exec curl -u $USER:$PASS -X MKCOL $URL/$FOLDER/$(echo {} | sed s#\./##) \;
find . -type f -exec curl -u $USER:$PASS -X PUT $URL/$FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

echo "Uploaded failing tests to https://www.kaminsky.me/nc-dev/android-integrationTests/$FOLDER"

curl -u $6:$7 -X POST https://api.github.com/repos/nextcloud/android/issues/$5/comments -d "{ \"body\" : \"$TYPE test failed: https://www.kaminsky.me/nc-dev/android-integrationTests/$FOLDER \" }"

exit 1
