#!/bin/bash -x

./gradlew gplayDebugExecuteScreenshotTests -Pandroid.testInstrumentationRunnerArguments.annotation=com.owncloud.android.utils.ScreenshotTest2
value=$?

if [ $value -ne 0 ]; then
    scripts/uploadReport.sh "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8"
    exit 1
fi

exit 0
