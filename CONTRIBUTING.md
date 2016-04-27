## Submitting issues

If you have questions about how to use ownCloud, please direct these to the [mailing list][mailinglist] or our [forum][forum]. We are also available on [IRC][irc].

### Guidelines
* [Report the issue](https://github.com/owncloud/android/issues/new) using our [template][template], it includes all the informations we need to track down the issue.
* This repository is *only* for issues within the ownCloud Android app code. Issues in other compontents should be reported in their own repositores: 
  - [ownCloud core](https://github.com/owncloud/core/issues)
  - [iOS client](https://github.com/owncloud/ios-issues/issues)
  - [Desktop client](https://github.com/owncloud/mirall/issues)
  - [ownCloud apps](https://github.com/owncloud/apps/issues) (e.g. Calendar, Contacts...)
* Search the [existing issues](https://github.com/owncloud/android/issues) first, it's likely that your issue was already reported.

If your issue appears to be a bug, and hasn't been reported, open a new issue.

Help us to maximize the effort we can spend fixing issues and adding new features, by not reporting duplicate issues.

[template]: https://raw.github.com/owncloud/android/master/issue_template.md
[mailinglist]: https://mailman.owncloud.org/mailman/listinfo/user
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

* Please follow [SETUP.md](https://github.com/owncloud/android/blob/master/SETUP.md) to setup ownCloud Android app work environment.


### 2. Create pull request:

NOTE: You must sign the [Contributor Agreement][agreement] before your changes can be accepted!

* Commit your changes locally: ```git commit -a```
* Push your changes to your GitHub repo: ```git push```
* Browse to https://github.com/YOURGITHUBNAME/android/pulls and issue pull request
* Enter description and send pull request.

### 3. Create another pull request:

To make sure your new pull request does not contain commits which are already contained in previous PRs, create a new branch which is a clone of upstream/master.

* ```git fetch upstream```
* ```git checkout -b my_new_master_branch upstream/master```
* If you want to rename that branch later: ```git checkout -b my_new_master_branch_with_new_name```
* Push branch to server: ```git push -u origin name_of_local_master_branch```
* Use GitHub to issue PR



## Translations
Please submit translations via [Transifex][transifex].

[transifex]: https://www.transifex.com/projects/p/owncloud/

