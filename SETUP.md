
These instructions will help you to set up your development environment, get the source code of the ownCloud for Android app and build it by yourself. If you want to help developing the app take a look to the [contribution guidelines][0].

Sections 1) and 2) are common for any environment. The rest of the sections describe how to set up a project in different tool environments. Choose the build tool or IDE you prefer and follow the instructions in its specific section. Nowadays we recommend to use Android Studio (section 2), but the decision is up to you.

If you have any problem, remove the 'android' folder, start again from 1) and work your way down. If something still does not work as described here, please open a new issue describing exactly what you did, what happened, and what should have happened.


### 0. Common software dependencies.

There are some tools needed, no matter what is your specific IDE or build tool of preference.

[git][1] is used to access to the different versions of the ownCloud's source code. Download and install the version appropiate for your operating system from [here][2]. Add the full path to the 'bin/' directory from your git installation into the PATH variable of your environment so that it can be used from any location.

The [Android SDK][3] is necessary to build the app. There are different options to install it in your system, depending of the IDE you decide to use. Check Google documentation about [installation][4] for more details on these options. After installing it, add the full path to the directories 'tools/' and 'platform-tools/' from your Android SDK installation into the PATH variable of your environment.

Open a terminal and type 'android' to start the Android SDK Manager. To build the ownCloud for Android app you will need to install at least the next SDK packages:

* Android SDK Tools and Android SDK Platform-tools (already installed); upgrade to their last versions is usually a good idea.
* Android SDK Build-Tools; any version from 20 or later should work fine; avoid preview versions, if any available.
* Android 4.4.2 (API 19), SDK Platform; needed for build/test ownCloud app.
* Android 5.1.1 (API 22), SDK Platform; needed to build the Android Support Library (not neeeded if working with Android Studio or gradle) and build the owncloud app.

Install any other package you consider interesting, such as emulators.

For other software dependencies check the details in the section corresponding to your preferred IDE or build system.


### 1. Fork and download the owncloud/android repository.

You will need [git][1] to access to the different versions of the ownCloud's source code. The source code is hosted in Github and may be read by anybody without needing a Github account. You will need a Github account if you want to contribute to the development of the app with your own code.

Next steps will assume you have a Github account and that you will get the code from your own fork. 

* In a web browser, go to https://github.com/owncloud/android, and click the 'Fork' button near the top right corner.
* Open a terminal and go on with the next steps in it.
* Clone your forked repository: ```git clone git@github.com:YOURGITHUBNAME/android.git```.
* Move to the project folder with ```cd android```.
* Checkout the remote branch 'master' in your own local branch 'master': ```git checkout master remotes/origin/master```.
* Pull any changes from your remote branch 'master': ```git pull origin master```
* Make official ownCloud repo known as upstream: ```git remote add upstream git@github.com:owncloud/android.git```
* Make sure to get the latest changes from official android/master branch: ```git pull upstream master```

At this point you can continue using different tools to build the project. Section 2, 3, 4, 5 and 6 describe the existing alternatives.


### 2. Working with Android Studio.

[Android Studio][5] is currently the official Android IDE. Due to this, we recommend it as the IDE to use in your development environment. Follow the installation instructions [here][6].

We recommend to use the last version available in the stable channel of Android Studio updates. See what update channel is your Android Studio checking for updates in the menu path 'Help'/'Check for Update...'/link 'Updates' in the dialog.

To set up the project in Android Studio follow the next steps:

* Complete the setup of project properties running:
    - Windows: ```setup_env.bat gradle```
    - Mac OS/Linux: ```./setup_env.sh gradle```
* Open Android Studio and select 'Import Project (Eclipse ADT, Gradle, etc)'. Browse through your file system to the folder 'android' where the project is located. Android Studio will then create the '.iml' files it needs. If you ever close the project but the files are still there, you just select 'Open Project...'. The file chooser will show an Android face as the folder icon, which you can select to reopen the project.
* Android Studio will try to build the project directly after importing it. To build it manually, follow the menu path 'Build'/'Make Project', or just click the 'Play' button in the tool bar to build and run it in a mobile device or an emulator. The resulting APK file will be saved in the 'build/outputs/apk/' subdirectory in the project folder.


### 3. Working in a terminal with Gradle:

[Gradle][7] is the build system used by Android Studio to manage the building operations on Android apps. You do not need to install Gradle in your system, and Google recommends not to do it, but instead trusting on the Graddle wrapper included in the project [8].

* Open a terminal and go to the 'android' directory that contains the repository.
* Complete the setup of project properties running:
    - Windows: ```setup_env.bat gradle```
    - Mac OS/Linux: ```./setup_env.sh gradle```
* Run the 'clean' and 'build' tasks using the Gradle wrapper provided
    - Windows: ```gradlew.bat clean build```
    - Mac OS/Linux: ```./gradlew clean build```
	
The first time the Gradle wrapper is called, the correct Gradle version will be downloaded automatically. An Internet connection is needed for it works.
	
The generated APK file is saved in android/build/outputs/apk as android-debug.apk


### 4. Building with Eclipse:

[Eclipse][9] is still an option to work with Android apps, although the [ADT Plugin][10] needed is not in active development anymore. Next steps have been tested in Eclipse Luna.

* Open a terminal and go to the 'android' directory that contains the repository.
* Resolve necessary dependencies running:
    - Windows: ```setup_env.bat ant```
    - Mac OS/Linux: ```./setup_env.sh ant```
* Open Eclipse and follow the menu path 'File'/'New'/'Project'
* Choose the option 'Android'/'Android Project from Existing Code' and click 'Next'
* Choose 'android/' folder as root
* Choose the projects with the next names under the 'New Project Name' column:
** owncloud-android 
** android-support-appcompat-v7-exploded-aar
** owncloud-android-workaround-accounts	(optional)
** ownCloud Android Library
** ownCloud Sample Client (optional)
** ownCloud Android library test project (optional)
** ownCloud Android library test cases (optional)
* Do not choose the project owncloud-android-tests; it's obsolete.
* Do not enable 'Copy projects into workspace'.
* Click the 'Finish' button.	
* Wait for a while; if 'Build automatically' is enabled in Eclipse, some errors could appear during the creation of the projects, but all of them should finally disappear.
* If any error persists, clean and build manually the next projects in order:
** ownCloud Android Library
** android-support-appcompat-v7-exploded-aar
** owncloud-android
* If any error on those projects persists, check the project properties. In the 'Android' section, API Level should be
** ownCloud Android Library	-> API level 19
** android-support-appcompat-v7-exploded-aa -> API level 22
** owncloud-android	-> API level 22 ; in this project, two library projects should appear referred in the bottom of the dialog: libs\android-support-appcompat-v7-exploded-aar and owncloud-android-library. Add them if needed.
* After those actions you should be good to go. HAVE FUN!


### 5. Building in command line with Ant:

[Ant][10] can be used to build the ownCloud for Android app in a terminal. Be sure that the PATH variable in your environment contains the full path to the 'bin/' subdirectory in your Ant installation. Define also an ANDROID_HOME variable in your environment with the full path to your Android SDK (see section 1). Then follow the next steps:

* Open a terminal and go to the 'android' directory that contains the repository.
* Resolve necessary dependencies running:
    - Windows: ```setup_env.bat ant```
    - Mac OS/Linux: ```./setup_env.sh ant```
* Run ```ant clean```.
* Run ```ant debug``` to generate a debuggable version of the ownCloud app.

The resulting APKs will be saved in the 'bin/' subdirectory of the project.


### 6. Building in command line with maven:

** Currently these build instructions DO NOT WORK. There is no estimation time to fix it. Unless some volunteer contributor fixes this build option, and given that Maven is a minority option in Android environments, we will probably remove this option.

NOTE: You must have mvn (version >= 3.1.1) in your environment path. Current Android 'platforms-tools' need to be installed.

Download/install Android plugin for Maven, then build ownCloud with mvn:

* Resolve necessary dependencies running:
    - Windows: "setup_env.bat maven"
    - Mac OS/Linux: "./setup_env.sh maven"
	
* cd ..
* git clone https://github.com/mosabua/maven-android-sdk-deployer.git
* cd maven-android-sdk-deployer
* mvn -pl com.simpligility.android.sdk-deployer:android-22 -am install
* cd ../android/owncloud-android-library
* mvn install
* cd ..

Now you can create ownCloud APK using "mvn package" and find it as ownCloud.apk under the target


[0]: https://github.com/owncloud/android/blob/master/CONTRIBUTING.md
[1]: https://git-scm.com/
[2]: https://git-scm.com/downloads
[3]: https://developer.android.com/sdk/index.html
[4]: https://developer.android.com/sdk/installing/index.html
[5]: https://developer.android.com/tools/studio/index.html
[6]: https://developer.android.com/sdk/installing/index.html?pkg=studio
[7]: https://gradle.org/
[8]: https://docs.gradle.org/current/userguide/gradle_wrapper.html
[9]: https://eclipse.org/
[10]: http://developer.android.com/sdk/installing/installing-adt.html
