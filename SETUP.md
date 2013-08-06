  
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

  -  TODO: FIX / MORE DETAILS
  -  Run "ant clean debug"
  -  Open Eclipse and import *actionbarsherlock/library* project to your workspace
  -  NOTE: You must have 'tools' and 'platforms-tools' in your path in order to run setup_env.sh
  -  After those actions you should be good to go. HAVE FUN!
  
  4. Create pull request:
  
  -  Commit your changes locally: "git commit -a"
  -  Push your changes to your Github repo: "git push"
  -  Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
  -  Click "Edit" and set "base:develop"
  -  Again, click "Edit" and set "compare:develop"
  -  Enter description and send pull request.




