# [Nextcloud](https://nextcloud.com) Android app
[![irc](https://img.shields.io/badge/IRC-%23nextcloud%20on%20freenode-orange.svg)](https://webchat.freenode.net/?channels=nextcloud)
[![irc](https://img.shields.io/badge/IRC-%23nextcloud-mobile%20on%20freenode-blue.svg)](https://webchat.freenode.net/?channels=nextcloud-mobile)

### Guidelines
* [Report the issue](https://github.com/nextcloud/android/issues/new) using our [template][template], it includes all the informations we need to track down the issue.
* This repository is *only* for issues within the Nextcloud Android app code. Issues in other compontents should be reported in their own repositores, e.g. [Nextcloud core](https://github.com/nextcloud/core/issues)
* Search the [existing issues](https://github.com/nextcloud/android/issues) first, it's likely that your issue was already reported.

If your issue appears to be a bug, and hasn't been reported, open a new issue.

## Contributing to Source Code

Thanks for wanting to contribute source code to Nextcloud. That's great!


### Guidelines
* Contribute your code in the branch 'master'. It will give us a better chance to test your code before merging it with stable code.
* For your first contribution, start a pull request on master and mention @nextcloud/android in your pull request.
* Keep on using pull requests for your next contributions although you own write permissions.

### 1. Fork and download android/master repository:

* Please follow [SETUP.md](https://github.com/nextcloud/android/blob/master/SETUP.md) to setup Nextcloud Android app work environment.


### 2. Create pull request:

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
...are an open issue. Please stay with us until we have bootstrapped translations.

