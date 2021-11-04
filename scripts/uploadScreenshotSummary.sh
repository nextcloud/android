#!/bin/bash -x

#1: LOG_USERNAME
#2: LOG_PASSWORD

DAV_URL=https://nextcloud.kaminsky.me/remote.php/webdav/android-screenshot-summary/
PUBLIC_URL=https://www.kaminsky.me/nc-dev/android-screenshot-summary
USER=$1
PASS=$2

date=$(date +%F)
echo "Uploaded screenshot summary to $PUBLIC_URL/$date/summary.html"

cd build/screenshotSummary

find . -type d -exec curl > /dev/null 2>&1 -u $USER:$PASS -X MKCOL $DAV_URL/$date/$(echo {} | sed s#\./##) \;
find . -type f -exec curl > /dev/null 2>&1 -u $USER:$PASS -X PUT $DAV_URL/$date/$(echo {} | sed s#\./##) --upload-file {} \;
