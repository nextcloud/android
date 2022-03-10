local testOnServer(serverBranch) = {
    "kind": "pipeline",
    "type": "docker",
    "name": "tests-"+serverBranch,
    "steps": [
        {
            "name": "gplay",
            "image": "ghcr.io/nextcloud/continuous-integration-android8:2",
            "privileged": "true"
        }
    ],
    "environment": [
        {
            "LOG_USERNAME": "from_secret: LOG_USERNAME",
            "LOG_PASSWORD": "from_secret: LOG_PASSWORD",
            "GIT_USERNAME": "from_secret: GIT_USERNAME",
            "GIT_TOKEN": "from_secret: GIT_TOKEN"
        }
    ],
    "commands": [
        "scripts/checkIfRunDrone.sh $GIT_USERNAME $GIT_TOKEN $DRONE_PULL_REQUEST || exit 0",
        "emulator -avd android -no-snapshot -gpu swiftshader_indirect -no-window -no-audio -skin 500x833 &",
        "sed -i s'#<bool name=\"is_beta\">false</bool>#<bool name=\"is_beta\">true</bool>#'g src/main/res/values/setup.xml",
        "sed -i s\"#server#server#\" gradle.properties",
        "./gradlew assembleGplay",
        "./gradlew assembleGplayDebug",
        "scripts/wait_for_emulator.sh",
        "scripts/deleteOldComments.sh 'stable' 'Unit' $DRONE_PULL_REQUEST $GIT_TOKEN",
        "./gradlew jacocoTestGplayDebugUnitTestReport || scripts/uploadReport.sh $LOG_USERNAME $LOG_PASSWORD $DRONE_BUILD_NUMBER 'stable' 'Unit' $DRONE_PULL_REQUEST $GIT_TOKEN",
        "./gradlew installGplayDebugAndroidTest",
        "scripts/wait_for_server.sh 'server'",
        "scripts/deleteOldComments.sh 'stable' 'IT' $DRONE_PULL_REQUEST $GIT_TOKEN",
        "./gradlew createGplayDebugCoverageReport -Pcoverage -Pandroid.testInstrumentationRunnerArguments.notAnnotation=com.owncloud.android.utils.ScreenshotTest || scripts/uploadReport.sh $LOG_USERNAME $LOG_PASSWORD $DRONE_BUILD_NUMBER 'stable' 'IT' $DRONE_PULL_REQUEST $GIT_TOKEN",
        "./gradlew combinedTestReport",
   ]     
};


local allScreenshots() = {
    "kind": "pipeline",
    "type": "docker",
    "name": "default",
    "steps": [
        {
            "name": "build",
            "image": "alpine",
            "commands": [
                "plain",
            ]
        }
    ]
};




[
testOnServer("stable-16"),
testOnServer("stable-23"),
testOnServer("master"),
allScreenshots(),
]

