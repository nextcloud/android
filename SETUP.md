  
  If you want to start help developing ownCloud please follow the [contribution guidlines][0] and observe these instructions:
  
  1. Fork and download android/develop repository:

  -  NOTE: You must have git in your enviroment path
  -  Navigate to https://github.com/owncloud/android, click fork.
  -  Clone your new repo: "git clone git@github.com:YOURGITHUBNAME/android.git"
  -  "cd android"
  -  Checkout remote develop branch: "git checkout -b develop remotes/origin/develop"
  -  Pull changes from your develop branch: "git pull origin develop"
  -  Make sure to get the latest changes from official android/develop branch:
  -  Make official owncloud repo known as upstream: "git remote add upstream git@github.com:owncloud/android.git"
  -  Pull latest changes from upstream: "git pull upstream develop"

  2. Building with console/maven:

  -  OPTIONAL, CONTINUE WITH STEP 3 IF NOT REQUIRED!
  -  NOTE: You must have mvn in your enviroment path
  -  Download/install Android plugin for Maven, then build ownCloud with mvn:
  -  "cd .."
  -  "git clone https://github.com/mosabua/maven-android-sdk-deployer.git"
  -  "cd maven-android-sdk-deployer"
  -  "mvn -pl com.simpligility.android.sdk-deployer:android-17 -am install"
  -  "cd ../android"
  -  Now you can create APK using "mvn package"

  3. Building with Eclipse:

  -  NOTE: You must have android/tools, and 'platforms-tools' in your enviroment path
  -  Prepare building with Eclipse:
  -  "setup_env.bat" or "./setup_env.sh"
  -  Open Eclipse and create new "Android Project from Existing Code". As root choose android/actionbarsherlock/library
  -  Increase Android API level until project compiles. 14 should work. 
  -  Clean project and compile.
  -  Make sure android/actionbarsherlock/library/bin/library.jar was created!
  -  Import OwnCloud Android project.
  -  Increase Android API level to 17.
  -  Clean project and compile.
  -  After those actions you should be good to go. HAVE FUN!
  -  NOTE: Even though API level is set to 17, APK also runs on older devices because in AndroidManifest.xml minSdkVersion is set to 8.

  4. Create pull request:
  
  -  NOTE: You must sign the [Contributor Agreement][1] before your changes can be accepted!
  -  Commit your changes locally: "git commit -a"
  -  Push your changes to your Github repo: "git push"
  -  Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
  -  Click "Edit" and set "base:develop"
  -  Again, click "Edit" and set "compare:develop"
  -  Enter description and send pull request.


[0]: https://github.com/owncloud/android/blob/master/CONTRIBUTING.md
[1]: http://owncloud.org/about/contributor-agreement/

