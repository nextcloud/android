  
  If you want to start development of ownCloud first download required files, then compile using console or Eclipse, finally create pull request:
  
  1. Fork and download android/develop repository:
  
  -  Navigate to https://github.com/owncloud/android, click fork.
  -  Clone your new repo: "git clone git@github.com:YOURGITHUBNAME/android.git"
  -  Checkout remote develop branch: "git checkout -b develop remotes/origin/develop"

  2. Building with console:

  -  Use setup_env.sh or setup_env.bat
  -  NOTE: You must have mvn, git, ant/bin, android/tools, and 'platforms-tools' in your enviroment path
  -  Now you can create APK using "mvn package"
  
  3. Building with eclipse:

  -  Open Eclipse and create new "Android Project from Existing Code". As root choose *actionbarsherlock/library*
  -  Increase Android API level until project compiles. 14 should work. bin/library.jar needs to be created!
  -  Import OwnCloud Android project.
  -  Increase Android API level to 17.
  -  Clean all projects.
  -  After those actions you should be good to go. HAVE FUN!
  -  TODO: How to build for older devices?
  
  4. Create pull request:
  
  -  Commit your changes locally: "git commit -a"
  -  Push your changes to your Github repo: "git push"
  -  Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
  -  Click "Edit" and set "base:develop"
  -  Again, click "Edit" and set "compare:develop"
  -  Enter description and send pull request.




