## Submitting issues

If you have questions about how to use ownCloud, please direct these to the [mailing list][mailinglist] or our [forum][forum]. We are also available on [IRC][irc].

### Guidelines
* [Report the issue](https://github.com/owncloud/android/issues/new) using our [template][template], it includes all the informations we need to track down the issue.
* This repository is *only* for issues within the ownCloud Android app code. Issues in other compontents should be reported in their own repositores: 
  - [ownCloud code](https://github.com/owncloud/core/issues)
  - [iOS client](https://github.com/owncloud/ios-issues/issues)
  - [Desktop client](https://github.com/owncloud/mirall/issues)
  - [ownCloud apps](https://github.com/owncloud/apps/issues) (e.g. Calendar, Contacts...)
* Search the existing issues first, it's likely that your issue was already reported.

If your issue appears to be a bug, and hasn't been reported, open a new issue.

Help us to maximize the effort we can spend fixing issues and adding new features, by not reporting duplicate issues.

[template]: https://raw.github.com/owncloud/android/master/issue_template.md
[mailinglist]: https://mail.kde.org/mailman/listinfo/owncloud
[forum]: http://forum.owncloud.org/
[irc]: http://webchat.freenode.net/?channels=owncloud&uio=d4

## Contributing to Source Code

Thanks for wanting to contribute source code to ownCloud. That's great!

Before we're able to merge your code into the ownCloud app for Android, you need to sign our [Contributor Agreement][agreement].

### Guidelines
* Contribute your code in the branch 'master'. It will give us a better chance to test your code before merging it with stable code.
* For your first contribution, start a pull request on master and send us the signed [Contributor Agreement][agreement].
* Keep on using pull requests for your next contributions although you own write permissions.

[agreement]: http://owncloud.org/about/contributor-agreement/

### 1. Fork and download android/master repository:

NOTE: You must have the git installation folder in your environment variable PATH to perform the next operations.

* In a web browser, go to https://github.com/owncloud/android, and click the 'Fork' button near the top right corner.
* In a command line prompt, clone your new repo: ```git clone git@github.com:YOURGITHUBNAME/android.git```.
* Move to the project folder with ```cd android```.
* Checkout the remote branch 'master' in your own local branch: ```git checkout -b master remotes/origin/master```.
* Pull any changes from your remote branch 'master': ```git pull origin master```
* Make official ownCloud repo known as upstream: ```git remote add upstream git@github.com:owncloud/android.git```
* Make sure to get the latest changes from official android/master branch: ```git pull upstream master```


### 7. Create pull request:

NOTE: You must sign the [Contributor Agreement][1] before your changes can be accepted!

* Commit your changes locally: "git commit -a"
* Push your changes to your GitHub repo: "git push"
* Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
* Enter description and send pull request.

### 8. Create another pull request:

To make sure your new pull request does not contain commits which are already contained in previous PRs, create a new branch which is a clone of upstream/master.

* git fetch upstream
* git checkout -b my_new_master_branch upstream/master
* If you want to rename that branch later: "git checkout -b my_new_master_branch_with_new_name"
* Push branch to server: "git push -u origin name_of_local_master_branch"
* Use GitHub to issue PR



## Translations
Please submit translations via [Transifex][transifex].

[transifex]: https://www.transifex.com/projects/p/owncloud/

