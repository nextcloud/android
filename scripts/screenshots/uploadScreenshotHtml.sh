#!/usr/bin/env bash
#1: LOG_USERNAME
#2: LOG_PASSWORD
#3: DRONE_BUILD_NUMBER
#4: BRANCH (stable or master)
#5: TYPE (IT or Unit)
#6: DRONE_PULL_REQUEST
#7: GIT_USERNAME
#8: GIT_TOKEN

URL=https://nextcloud.kaminsky.me/remote.php/webdav/android-integrationTests
USER=$1
PASS=$2
REMOTE_FOLDER=screenshotOverview-$(date +%F)

cd build/screenshotOverview
find . -type d -exec curl -u $USER:$PASS -X MKCOL $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) \;
find . -type f -exec curl -u $USER:$PASS -X PUT $URL/$REMOTE_FOLDER/$(echo {} | sed s#\./##) --upload-file {} \;

echo "Uploaded screenshot overview to https://www.kaminsky.me/nc-dev/android-integrationTests/$REMOTE_FOLDER"

exit 0
