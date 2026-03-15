#!/bin/bash

# SPDX-FileCopyrightText: 2021-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2021-2023 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

DRONE_PULL_REQUEST=$1
LOG_USERNAME=$2
LOG_PASSWORD=$3
DRONE_BUILD_NUMBER=$4

function upload_logcat() {
    log_filename="${DRONE_PULL_REQUEST}_logcat.txt.xz"
    log_file="app/build/${log_filename}"
    upload_path="https://nextcloud.kaminsky.me/remote.php/webdav/android-logcat/$log_filename"
    xz logcat.txt
    mv logcat.txt.xz "$log_file"
    curl -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT "$upload_path" --upload-file "$log_file"
    echo >&2 "Uploaded logcat to https://www.kaminsky.me/nc-dev/android-logcat/$log_filename"
}

scripts/deleteOldComments.sh "master" "IT" "$DRONE_PULL_REQUEST"

./gradlew assembleGplayDebugAndroidTest

scripts/wait_for_emulator.sh || exit 1

./gradlew installGplayDebugAndroidTest
scripts/wait_for_server.sh "server" || exit 1

# clear logcat and start saving it to file
adb logcat -c
adb logcat > logcat.txt &
LOGCAT_PID=$!
./gradlew connectedGplayDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.owncloud.android.utils.ScreenshotTest \
-Dorg.gradle.jvmargs="--add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.nio.channels=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"

stat=$?
# stop saving logcat
kill $LOGCAT_PID

if [ ! $stat -eq 0 ]; then
    upload_logcat
    bash scripts/uploadReport.sh "$LOG_USERNAME" "$LOG_PASSWORD" "$DRONE_BUILD_NUMBER" "master" "IT" "$DRONE_PULL_REQUEST"
fi

echo "Exit with: " $stat
exit $stat
