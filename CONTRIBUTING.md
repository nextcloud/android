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
        1. Beta
    1. Version Name and number
    1. Release cycle
    1. Release Process
        1. Stable
        1. Release Candidate
        1. Development Beta


# Guidelines

## Issue reporting
* [Report the issue](https://github.com/nextcloud/android/issues/new) using our [template][template], it includes all the information we need to track down the issue.
* This repository is *only* for issues within the Nextcloud Android app code. Issues in other components should be reported in their own repositories, e.g. [Nextcloud core](https://github.com/nextcloud/core/issues)
* Search the [existing issues](https://github.com/nextcloud/android/issues) first, it's likely that your issue was already reported.
If your issue appears to be a bug, and hasn't been reported, open a new issue.


## Labels


### Pull request
* 1 to develop
* 2 developing
* 3 to review
* 4 to release


### Issue
* nothing
* approved
* PR exists (and then the PR# should be shown in first post)


# Contributing to Source Code
Thanks for wanting to contribute source code to Nextcloud. That's great!


## Developing process
We are all about quality while not sacrificing speed so we use a very pragmatic workflow.

* create an issue with feature request
    * discuss it with other developers 
    * create mockup if necessary
    * must be approved --> label approved
    * after that no conceptual changes!
* develop code
* create [pull request](https://github.com/nextcloud/android/pulls)
* to assure the quality of the app, any PR gets reviewed, approved and tested by [two developers](https://github.com/nextcloud/android/blob/master/MAINTAINERS) before it will be merged to master

### Android Studio formatter setup

Our formatter setup is rather simple:
* Standard Android Studio
* Line length 120 characters (Settings->Editor->Code Style->Right margin(columns): 120)
* Auto optimize imports (Settings->Editor->Auto Import->Optimize imports on the fly)


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
Play store and f-droid releases for the masses
stable: as described, PRs that have been tested and reviewed can go to master. After the last stable beta published PR is out in the wild for ~2 weeks and no errors get reported (by users or in the developer console) the master branch is ready for the stable release. So when we decide to go for a new release we freeze the master feature wise.

### Release Candidate
* _stable beta_ releases done via the Beta program of the Google Play store
stable beta: whenever a PR is reviewed/approved we put it on master and do a stable beta release
release candidate = tested PRs, merged to to master between stable releases, published on the Play store beta channel

### Development Beta
* _development beta_ releases done as a standalone app that can be installed in parallel to the stable app
beta: anything that has a certain maturity as in a PR that can be used already but might lack some on top features or polishing
beta = your awesome beta application that can be installed in parallel and contains PRs that are done in development but not necessarily to be considered stable enough for master or might even still have known bugs


##Version Name and number
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


## Release cycle
* for each release we choose several PRs that will be included in the next release. Currently there are many open PRs from ownCloud, but after merging them, the intention is to choose the PRs that are ready (reviewed, tested) to get them merged very soon.
* these will be merged into master, tested heavily, maybe automatic testing
* after feature freeze a public play store beta is released
* ~2 weeks testing, bug fixing
* release final version on f-droid and play store

To get an idea which PRs and issues will be part of the next release simply check our [milestone plan](https://github.com/nextcloud/android/milestones)

##Release process


###Stable Release
Stable releases are based on the git [master](https://github.com/nextcloud/android).

1. Bump the version name and version code in the [AndroidManifest.xml](https://github.com/nextcloud/android/blob/master/AndroidManifest.xml), see chapter 'Version Name and number'.
2. Create a [release/tag](https://github.com/nextcloud/android/releases) in git. Tag name following the naming schema: ```stable-Mayor.Minor.Hotfix``` (e.g. stable-1.2.0) naming the version number following the [semantic versioning schema](http://semver.org/)


###Release Candidate Release
Release Candidate releases are based on the git [master](https://github.com/nextcloud/android) and are done between stable releases.

1. Bump the version name and version code in the [AndroidManifest.xml](https://github.com/nextcloud/android/blob/master/AndroidManifest.xml), see below the version name and code concept.
2. Create a [release/tag](https://github.com/nextcloud/android/releases) in git. Tag name following the naming schema: ```rc-Mayor.Minor.Hotfix-betaIncrement``` (e.g. rc-1.2.0-12) naming the version number following the [semantic versioning schema](http://semver.org/)


###Development Beta Release
Beta releases are based on the git [beta](https://github.com/nextcloud/android/tree/beta) and are done independently from stable releases and integrate open PRs that might not be production ready or heavily tested but being put out there for people willing to test new features and provide valuable feedback on new features to be incorporated before a feature gets released in the stable app.

1. Bump the version name and version code in the [AndroidManifest.xml](https://github.com/nextcloud/android/blob/master/AndroidManifest.xml), see below the version name and code concept.
2. Create a [release/tag](https://github.com/nextcloud/android/releases) in git. Tag name following the naming schema: ```beta-YYYYMMDD``` (e.g. beta-20160612)
