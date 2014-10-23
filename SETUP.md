  
If you want to start help developing ownCloud please follow the [contribution guidelines][0] and observe these instructions.

If you have any problems, start again with 1) and work your way down. If something still does not work as described here, please open a new issue describing exactly what you did, what happened, and what should have happened.
  
### 1) Fork and download android/develop repository:

NOTE: Android SDK with platforms 8, 14 and 19 (and maybe others) need to be installed.
      You must have the Android SDK 'tools/', and 'platforms-tools/' folders in your environment path variable.
      "git" need to be installed and in your environment path variable.

* Navigate to https://github.com/owncloud/android, click fork.
* Clone your new repo: "git clone git@github.com:YOURGITHUBNAME/android.git"
* Move to the project folder with "cd android"
* Checkout remote develop branch: "git checkout -b develop remotes/origin/develop"
* Pull changes from your develop branch: "git pull origin develop"
* Make official ownCloud repo known as upstream: "git remote add upstream git@github.com:owncloud/android.git"
* Make sure to get the latest changes from official android/develop branch: "git pull upstream develop"
* Complete the setup of project properties and resolve pending dependencies running "setup_env.bat" or "./setup_env.sh" .

At this point you can continue using different tools to build the project. Sections 2a), 2b), and 2c) describe some of the existing alternatives.

### 2a) Building with Ant:
  
NOTE: You must have the Android SDK 'tools/', and 'platforms-tools/' folders in your environment path variable.

* Run "ant clean" .
* Run "ant debug" to generate a debuggable version of the ownCloud app.

### 2b) Building with console/maven:

NOTE: You must have mvn (version >= 3.1.1) in your environment path. Current Android 'platforms-tools' need to be installed.

Download/install Android plugin for Maven, install owncloud-android-library, then build ownCloud with mvn:

* cd ..
* git clone https://github.com/mosabua/maven-android-sdk-deployer.git
* cd maven-android-sdk-deployer
* mvn -pl com.simpligility.android.sdk-deployer:android-19 -am install
* cd ../android/owncloud-android-library
* mvn install
* cd ..

Now you can create ownCloud APK using "mvn package"

### 2c) Building with Eclipse:

NOTE: You must have the Android SDK 'tools/', and 'platforms-tools/' folders in your environment path variable.

* Complete the setup of project properties and resolve pending dependencies running "setup_env.bat" or "./setup_env.sh" .
* Open Eclipse and create new "Android Project from Existing Code". Choose android/actionbarsherlock/library as root.
* Clean project and compile.
* If any error appear, check the project properties; in the 'Android' section, API Level should be greater or equal than 14.
* If "error loading libz.so.1" appears, try "sudo apt-get install lib32z1"
* Make sure android/actionbarsherlock/library/bin/library.jar was created.
* Create a new "Android Project from Existing Code". Choose android/owncloud-android-library as root. (test and sample clients are not required.)
* Clean project and compile.
* If any error appear, check the project properties; in the 'Android' section, API Level should be 19 or greater.
* Make sure 'android/owncloud-android-library/bin/owncloud android library.jar' was created.
* Import ownCloud Android project.
* Clean project and compile.
* If any error appears, check the project properties of owncloud-android project; in the 'Android' section:
  - API Level should be 19 or greater.
  - Two library projects should appear referred in the bottom square: actionbarsherlock/library and owncloud-android-library. Add them if needed. 
* After those actions you should be good to go. HAVE FUN!

NOTE: Even though API level is set to 19, APK also runs on older devices because in AndroidManifest.xml minSdkVersion is set to 8.

### 3) Create pull request:
  
NOTE: You must sign the [Contributor Agreement][1] before your changes can be accepted!

* Commit your changes locally: "git commit -a"
* Push your changes to your Github repo: "git push"
* Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
* Click "Edit" and set "base:develop"
* Again, click "Edit" and set "compare:develop"
* Enter description and send pull request.

### 4) Create another pull request:

To make sure your new pull request does not contain commits which are already contained in previous PRs, create a new branch which is a clone of upstream/develop.

* git fetch upstream
* git checkout -b my_new_develop_branch upstream/develop
* If you want to rename that branch later: "git checkout -b my_new_develop_branch_with_new_name"
* Push branch to server: "git push -u origin name_of_local_develop_branch"
* Use Github to issue PR


[0]: https://github.com/owncloud/android/blob/master/CONTRIBUTING.md
[1]: http://owncloud.org/about/contributor-agreement/
