These instructions will help you to set up your development environment, get the source code of the Nextcloud for Android app and build it by yourself. If you want to help developing the app take a look to the [contribution guidelines][0].

Sections 1) and 2) are common for any environment. The rest of the sections describe how to set up a project in different tool environments. Nowadays we recommend to use Android Studio (section 2), but you can also build the app from the command line (section 3).

If you have any problem, remove the 'android' folder, start again from 1) and work your way down. If something still does not work as described here, please open a new issue describing exactly what you did, what happened, and what should have happened.


### 0. Common software dependencies.

There are some tools needed, no matter what is your specific IDE or build tool of preference.

[git][1] is used to access to the different versions of the Nextcloud's source code. Download and install the version appropriate for your operating system from [here][2]. Add the full path to the 'bin/' directory from your git installation into the PATH variable of your environment so that it can be used from any location.

The [Android SDK][3] is necessary to build the app. There are different options to install it in your system, depending of the IDE you decide to use. Check Google documentation about [installation][4] for more details on these options. After installing it, add the full path to the directories 'tools/' and 'platform-tools/' from your Android SDK installation into the PATH variable of your environment.

Open a terminal and type 'android' to start the Android SDK Manager. To build the Nextcloud for Android app you will need to install at least the next SDK packages:

* Android SDK Tools and Android SDK Platform-tools (already installed); upgrade to their last versions is usually a good idea.
* Android SDK Build-Tools 24.0.2.
* Android 7.0 (API 24), SDK Platform; needed to build the nextcloud app.

Install any other package you consider interesting, such as emulators.

For other software dependencies check the details in the section corresponding to your preferred IDE or build system.


### 1. Fork and download the nextcloud/android repository.

You will need [git][1] to access to the different versions of the Nextcloud's source code. The source code is hosted on GitHub and may be read by anybody, without a GitHub account. You will need one if you want to contribute to the development of the app with your own code.

The next steps will assume you have a GitHub account and that you will get the code from your own fork.

* In a web browser, go to https://github.com/nextcloud/android, and click the 'Fork' button near the top right corner.
* Open a terminal and go on with the next steps in it.
* Clone your forked repository: ```git clone https://github.com/YOURGITHUBNAME/android.git```.
* Move to the project folder with ```cd android```.
* Pull any changes from your remote branch 'master': ```git pull origin master```
* Make official Nextcloud repo known as upstream: ```git remote add upstream https://github.com/nextcloud/android.git```
* Make sure to get the latest changes from official android/master branch: ```git pull upstream master```

At this point you can continue using different tools to build the project. Section 2 and 3 describe the existing alternatives.


### 2. Working with Android Studio.

[Android Studio][5] is currently the official Android IDE. Due to this, we recommend it as the IDE to use in your development environment. Follow the installation instructions [here][6].

We recommend to use the last version available in the stable channel of Android Studio updates. See what update channel is your Android Studio checking for updates in the menu path 'Help'/'Check for Update…'/link 'Updates' in the dialog.

To set up the project in Android Studio follow the next steps:

* Open Android Studio and select 'Import Project (Eclipse ADT, Gradle, etc)'. Browse through your file system to the folder 'android' where the project is located. Android Studio will then create the '.iml' files it needs. If you ever close the project but the files are still there, you just select 'Open Project…'. The file chooser will show an Android face as the folder icon, which you can select to reopen the project.
* Android Studio will try to build the project directly after importing it. To build it manually, follow the menu path 'Build'/'Make Project', or just click the 'Play' button in the toolbar to build and run it in a mobile device or an emulator. The resulting APK file will be saved in the 'build/outputs/apk/' subdirectory in the project folder.
* Setup Android Studio editor configurtation for the project: ```Settings``` → ```Editor``` → ```Code Style``` → ```Scheme: Project``` and ```Enable EditorConfig support```


### 3. Working in a terminal with Gradle:

[Gradle][7] is the build system used by Android Studio to manage the building operations on Android apps. You do not need to install Gradle in your system, and Google recommends not to do it, but instead trusting on the [Gradle wrapper][8] included in the project.

* Open a terminal and go to the 'android' directory that contains the repository.
* Run the 'clean' and 'build' tasks using the Gradle wrapper provided
    - Windows: ```gradlew.bat clean build```
    - Mac OS/Linux: ```./gradlew clean build```

The first time the Gradle wrapper is called, the correct Gradle version will be downloaded automatically. This requires a working Internet connection.

The generated APK file is saved in android/build/outputs/apk as android-debug.apk

### 4. App flavours

The app is currently equipped to be built with three flavours:
* generic - the regular build, released as Nextcloud Android app on FDroid
* gplay - with Google Stuff (Push notification), used for Google Play Store
* versionDev - based on master and library master, available as direct download and FDroid

[0]: https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md
[1]: https://git-scm.com/
[2]: https://git-scm.com/downloads
[3]: https://developer.android.com/sdk/index.html
[4]: https://developer.android.com/sdk/installing/index.html
[5]: https://developer.android.com/tools/studio/index.html
[6]: https://developer.android.com/sdk/installing/index.html?pkg=studio
[7]: https://gradle.org/
[8]: https://docs.gradle.org/current/userguide/gradle_wrapper.html

### 5. How-To

#### 1. Direct usage of library project

This is handy if one wants to make changes both to files app and library:
- in files app root: ln -s $pathToLibraryProject nextcloud-android-library
- uncomment in build.gradle:  
    - `//    implementation project('nextcloud-android-library')`
- comment in build.gradle: 
    - `genericImplementation 'com.github.nextcloud:android-library:master-SNAPSHOT'`
    - `gplayImplementation 'com.github.nextcloud:android-library:master-SNAPSHOT'`
    - `versionDevImplementation 'com.github.nextcloud:android-library:master-SNAPSHOT'`
- comment in settings.gradle: 
    - `include ':'`
- uncomment in settings.gradle: 
    - `//include 'nextcloud-android-library'`
- sync project with gradle files

Now every change in library can be directly used in files app.

### 6. Troubleshooting

#### 1. Compilation fails with "java.lang.OutOfMemoryError: Java heap space" error
The default settings for Gradle is to limit the compilation to 1GB of heap.
You can increase that value by :
- adding `org.gradle.jvmargs=-Xmx4G` to `gradle.properties`
- running gradlew(.bat) with this command line : `GRADLE_OPTS="-Xmx4G" ./gradlew clean build"

