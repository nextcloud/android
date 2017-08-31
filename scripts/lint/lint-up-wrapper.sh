#!/bin/sh

#1: GIT_USERNAME
#2: GIT_TOKEN
#3: BRANCH
#4: LOG_USERNAME
#5: LOG_PASSWORD
#6: DRONE_BUILD_NUMBER

ruby scripts/lint/lint-up.rb $1 $2 $3

returnValue=$?
if [ $returnValue -eq 0 ]; then
    echo "New master at: https://nextcloud.kaminsky.me/index.php/s/tXwtChzyqMj6I8v"
    curl -u $4:$5 -X PUT https://nextcloud.kaminsky.me/remote.php/webdav/droneLogs/master.html --upload-file build/reports/lint/lint.html
    exit 0
elif [ $returnValue -eq 1 ]; then
    echo "New results at https://nextcloud.kaminsky.me/index.php/s/tXwtChzyqMj6I8v ->" $6.html
    curl -u $4:$5 -X PUT https://nextcloud.kaminsky.me/remote.php/webdav/droneLogs/$6.html --upload-file build/reports/lint/lint.html
    exit 1
fi
