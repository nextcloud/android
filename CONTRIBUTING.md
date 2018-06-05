# [Nextcloud](https://nextcloud.com) Android app

[![irc](https://img.shields.io/badge/IRC-%23nextcloud%20on%20freenode-orange.svg)](https://webchat.freenode.net/?channels=nextcloud)
[![irc](https://img.shields.io/badge/IRC-%23nextcloud--mobile%20on%20freenode-blue.svg)](https://webchat.freenode.net/?channels=nextcloud-mobile)


# Index
1. Guidelines
    1. Issue reporting
    1. Labels
        1. PR
        1. Issue
1. Contributing to Source Code
    1. Developing process
        1. Android Studio formatter setup
    1. Contribution process
        1. Fork and download android/master repository
        1. Create pull request
        1. Create another pull request
    1. Translations
1. Releases
    1. Types
        1. Stable
        1. Release Candidate
        1. Dev
    1. Version Name and number
    1. Release cycle
    1. Release Process
        1. Stable
        1. Release Candidate
        1. Development Dev


# Guidelines

## Issue reporting
* [Report the issue](https://github.com/nextcloud/android/issues/new) using our [template](https://github.com/nextcloud/android/blob/master/issue_template.md), it includes all the information we need to track down the issue.
* This repository is *only* for issues within the Nextcloud Android app code. Issues in other components should be reported in their own repositories, e.g. [Nextcloud core](https://github.com/nextcloud/core/issues)
* Search the [existing issues](https://github.com/nextcloud/android/issues) first, it's likely that your issue was already reported.
If your issue appears to be a bug, and hasn't been reported, open a new issue.


## Labels


### Pull request
* 1 developing
* 2 to review
* 3 to release


### Issue
* nothing
* approved
* PR exists (and then the PR# should be shown in first post)


### Bug workflow
* approved: at least one other is able to reproduce it
* needs info: something unclear, or not able to reproduce
  * if no response within 1 months, bug will be closed
* pr exists: if bug is fixed, link to pr


# Contributing to Source Code
Thanks for wanting to contribute source code to Nextcloud. That's great!

New contributions are added under AGPL version 3.

## Developing process
We are all about quality while not sacrificing speed so we use a very pragmatic workflow.

* create an issue with feature request
    * discuss it with other developers
    * create mockup if necessary
    * must be approved --> label approved
    * after that no conceptual changes!
* develop code
* create [pull request](https://github.com/nextcloud/android/pulls)
* to assure the quality of the app, any PR gets reviewed, approved and tested by [two developers](https://github.com/nextcloud/android/blob/master/.pullapprove.yml#L29) before it will be merged to master


### Branching model
![branching model](/doc/branching.png "Branching Model")
* All contributions bug fix or feature PRs target the ```master``` branch
* Feature releases will always be based on ```master```
* Bug fix releases will always be based on their respective feature-release-bug-fix-branches
* Bug fixes relevant for the most recent _and_ released feature (e.g. ```2.0.0```) or bugfix (e.g. ```2.0.1```) release will be backported to the respective bugfix branch (e.g. ```2.0.x``` or ```2.1.x```)
* Hot fixes not relevant for an upcoming feature release but the latest release can target the bug fix branch directly


### Android Studio formatter setup
Our formatter setup is rather simple:
* Standard Android Studio
* Line length 120 characters (Settings->Editor->Code Style->Right margin(columns): 120)
* Auto optimize imports (Settings->Editor->Auto Import->Optimize imports on the fly)


### Build variants
There are three build variants
* generic: no Google Stuff, used for FDroid
* gplay: with Google Stuff (Push notification), used for Google Play Store
* versionDev: based on master and library master, available as direct download and FDroid

## Contribution process
* Contribute your code in the branch 'master'. It will give us a better chance to test your code before merging it with stable code.
* For your first contribution start a pull request on master.


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
We manage translations via [Transifex](https://www.transifex.com/nextcloud/nextcloud/android/). So just request joining the translation team for Android on the site and start translating. All translations will then be automatically pushed to this repository, there is no need for any pull request for translations.

# Releases
At the moment we are releasing the app in two app stores:

* [Google Play Store](https://play.google.com/store/apps/details?id=com.nextcloud.client)
* [f-droid](https://f-droid.org/repository/browse/?fdfilter=com.nextcloud)


## Types
We do differentiate between three different kinds of releases:

### Stable
Play store and f-droid releases for the masses.
Pull Requests that have been tested and reviewed can go to master. After the last release candidate is out in the wild for ~2 weeks and no errors get reported (by users or in the developer console) the master branch is ready for the stable release.
So when we decide to go for a new release we freeze the master feature wise.

### Release Candidate
_stable beta_ releases done via the Beta program of the Google Play store and f-droid.
Whenever a PR is reviewed/approved we put it on master.
Before releasing a new stable version there is at least one release candidate. It is based on the current master and during this phase the master is feature freezed. After ~2 weeks with no error a stable version will be released, which is identical to the latest release candidate. 

### Dev
Done as a standalone app that can be installed in parallel to the stable app.
Any PR which is labelled "ready for dev" will be automatically included in the dev app. This label should only set by the main developers.
Same applies for the android-library. This repository also has a branch called dev which includes all upcoming features. The dev branch on this repository must always use the android-library dev branch.

## Version Name and number
### Stable / Release candidate
For _stable_ and _release candidate_ the version name follows the [semantic versioning schema](http://semver.org/) and the version number has several digits reserved to parts of the versioning schema inspired by the [jayway version numbering](https://www.jayway.com/2015/03/11/automatic-versioncode-generation-in-android-gradle/), where:

* 2 digits for beta code as in release candidates starting at '01'
* 2 digits for hot fix code
* 3 digits for minor version code
* n digits for mayor version code

![Version code schema](https://cloud.githubusercontent.com/assets/1315170/15992040/e4e05442-30c2-11e6-88e2-84e77fa1653d.png)

Examples for different versions:
* 1.0.0 ```10000099```
* 8.12.2 ```80120200```
* 9.8.4-rc18 ```90080418```

beware, that beta releases for an upcoming version will always use the minor and hotfix version of the release they are targeting. So to make sure the version code of the upcoming stable release will always be higher stable releases set the 2 beta digits to '99' as seen above in the examples.

### Dev
For dev the version name is in format YYYYMMDD. It is mainly as a reference for reporting bugs and is not related to stable/release candidates as it is an independent app.

## Release cycle
* for each release we choose several PRs that will be included in the next release. Currently there are many open PRs from ownCloud, but after merging them, the intention is to choose the PRs that are ready (reviewed, tested) to get them merged very soon.
* these will be merged into master, tested heavily, maybe automatic testing
* after feature freeze a public release candidate on play store and f-droid is released
* ~2 weeks testing, bug fixing
* release final version on f-droid and play store

To get an idea which PRs and issues will be part of the next release simply check our [milestone plan](https://github.com/nextcloud/android/milestones)

## Release process


### Stable Release
Stable releases are based on the git [master](https://github.com/nextcloud/android).

1. Bump the version name and version code in the [AndroidManifest.xml](https://github.com/nextcloud/android/blob/master/AndroidManifest.xml), see chapter 'Version Name and number'.
2. Create a [release/tag](https://github.com/nextcloud/android/releases) in git. Tag name following the naming schema: ```stable-Mayor.Minor.Hotfix``` (e.g. stable-1.2.0) naming the version number following the [semantic versioning schema](http://semver.org/)


### Release Candidate Release
Release Candidate releases are based on the git [master](https://github.com/nextcloud/android) and are done between stable releases.

1. Bump the version name and version code in the [AndroidManifest.xml](https://github.com/nextcloud/android/blob/master/AndroidManifest.xml), see below the version name and code concept.
2. Create a [release/tag](https://github.com/nextcloud/android/releases) in git. Tag name following the naming schema: ```rc-Mayor.Minor.Hotfix-betaIncrement``` (e.g. rc-1.2.0-12) naming the version number following the [semantic versioning schema](http://semver.org/)


### Dev Release
Dev releases are based on the [master](https://github.com/nextcloud/android/tree/master) branch and are done independently from stable releases for people willing to test new features and provide valuable feedback on new features to be incorporated before a feature gets released in the stable app.

The deployment/build is done once a day automatically. If code has changed a new apk will be published [here](https://download.nextcloud.com/android/dev) and it will, with a little delay, be available on [Fdroid](https://f-droid.org/repository/browse/?fdfilter=nextcloud&fdid=com.nextcloud.android.beta).
