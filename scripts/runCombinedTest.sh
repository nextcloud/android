#!/bin/bash

GIT_USERNAME=$1
GIT_TOKEN=$2
DRONE_PULL_REQUEST=$3
LOG_USERNAME=$4
LOG_PASSWORD=$5
DRONE_BUILD_NUMBER=$6

scripts/deleteOldComments.sh "master" "Unit" $DRONE_PULL_REQUEST $GIT_TOKEN
scripts/deleteOldComments.sh "master" "IT" $DRONE_PULL_REQUEST $GIT_TOKEN

./gradlew assembleGplay
./gradlew assembleGplayDebug

scripts/wait_for_emulator.sh
./gradlew jacocoTestGplayDebugUnitTestReport 
stat=$?

if [ ! $stat -eq 0 ]; then
    bash scripts/uploadReport.sh $LOG_USERNAME $LOG_PASSWORD $DRONE_BUILD_NUMBER "master" "Unit" $DRONE_PULL_REQUEST $GIT_TOKEN
fi

./gradlew installGplayDebugAndroidTest
scripts/wait_for_server.sh "server"
./gradlew createGplayDebugCoverageReport -Pcoverage -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.owncloud.android.utils.ScreenshotTest
stat=$(( stat | $? ))

if [ ! $stat -eq 0 ]; then
    bash scripts/uploadReport.sh $LOG_USERNAME $LOG_PASSWORD $DRONE_BUILD_NUMBER "master" "IT" $DRONE_PULL_REQUEST $GIT_TOKEN
fi

./gradlew combinedTestReport
stat=$(( stat | $? ))

curl -Os https://uploader.codecov.io/latest/linux/codecov
chmod +x codecov
./codecov -t fc506ba4-33c3-43e4-a760-aada38c24fd5

echo "Exit with: " $stat
exit $stat
