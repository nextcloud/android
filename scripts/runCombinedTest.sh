#!/bin/bash

DRONE_PULL_REQUEST=$1
LOG_USERNAME=$2
LOG_PASSWORD=$3
DRONE_BUILD_NUMBER=$4

scripts/deleteOldComments.sh "master" "IT" $DRONE_PULL_REQUEST

./gradlew assembleGplayDebugAndroidTest

scripts/wait_for_emulator.sh

./gradlew installGplayDebugAndroidTest
scripts/wait_for_server.sh "server"
./gradlew createGplayDebugCoverageReport -Pcoverage -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.owncloud.android.utils.ScreenshotTest
stat=$?

if [ ! $stat -eq 0 ]; then
    bash scripts/uploadReport.sh $LOG_USERNAME $LOG_PASSWORD $DRONE_BUILD_NUMBER "master" "IT" $DRONE_PULL_REQUEST
fi

curl -Os https://uploader.codecov.io/latest/linux/codecov
chmod +x codecov
./codecov -t fc506ba4-33c3-43e4-a760-aada38c24fd5 -F integration

echo "Exit with: " $stat
exit $stat
