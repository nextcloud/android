#!/usr/bin/env bash

## emulator
if [[ ! $(emulator -list-avds | grep uiComparison -c) -eq 0 ]]; then
    avdmanager delete avd -n uiComparison
    (sleep 5; echo "no") | avdmanager create avd -n uiComparison -c 100M -k "system-images;android-27;google_apis;x86" --abi "google_apis/x86"
fi

if [ "$1" == "debug" ]; then
  emulator -writable-system -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-audio -skin 500x833 1>/dev/null &
else
  emulator -writable-system -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-window -no-audio -skin 500x833 1>/dev/null &
fi
PID=$!

scripts/wait_for_emulator.sh

sed -i s'#<bool name="is_beta">false</bool>#<bool name="is_beta">true</bool>#'g src/main/res/values/setup.xml

## update/create all screenshots
#./gradlew gplayDebugExecuteScreenshotTests -Precord \
#-Pandroid.testInstrumentationRunnerArguments.annotation=com.owncloud.android.utils.ScreenshotWithoutServerTest

## update screenshots in a class
#./gradlew gplayDebugExecuteScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest

## update single screenshot within a class
#./gradlew gplayDebugExecuteScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.nextcloud.client.FileDisplayActivityIT#showShares

resultCode=-1
retryCount=0
until [ $resultCode -eq 0 ] || [ $retryCount -gt 2 ]
do
  # test all screenshots
  ./gradlew gplayDebugRemoveScreenshots
  ./gradlew gplayDebugExecuteScreenshotTests \
  -Precord \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.owncloud.android.utils.ScreenshotWithoutServerTest

resultCode=$?
((retryCount++))
done

sed -i s'#<bool name="is_beta">true</bool>#<bool name="is_beta">false</bool>#'g src/main/res/values/setup.xml

if [ "$1" == "debug" ]; then
  exit
fi

# tidy up
kill "$PID"
