** Work in progress

This project contains a set of automatic tests operating in the UI level.

Tests are to be run with the tool Appium. Check [here][0] to install it and all its dependencies (including Maven).

You will need to modify the constants in automationTest/src/test/java/com/owncloud/android/test/ui/testSuites/Config.java to assign appropriate values for your test server and accounts.
You will need to include the ownCloud.apk to test in automationTest/src/test/resources/.

To run the tests from command line, plug a device to your computer or start and emulator. Then type 

mvn clean tests

To run only one category of the test

mvn clean -Dtest=RunSmokeTests test

The project may also be imported in Eclipse, with the appropriate plug-ins, and run from it.

[0]: http://appium.io/slate/en/master/?java#about-appium