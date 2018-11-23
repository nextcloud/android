# [Nextcloud](https://nextcloud.com) Android app 

[![Build Status](https://drone.nextcloud.com/api/badges/nextcloud/android/status.svg)](https://drone.nextcloud.com/nextcloud/android) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/80401cb343854343b4d94acbfb72d3ec)](https://www.codacy.com/app/Nextcloud/android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=nextcloud/android&amp;utm_campaign=Badge_Grade) [![Releases](https://img.shields.io/github/release/nextcloud/android.svg)](https://github.com/nextcloud/android/releases/latest) [![irc](https://img.shields.io/badge/IRC-%23nextcloud--mobile%20on%20freenode-blue.svg)](https://webchat.freenode.net/?channels=nextcloud-mobile)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" 
      alt="Download from Google Play" 
      height="80">](https://play.google.com/store/apps/details?id=com.nextcloud.client)
[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.nextcloud.client/)

**The Android client for [Nextcloud](https://nextcloud.com). Easily work with your data on your Nextcloud.**

![App screenshots](/doc/Nextcloud_Android_Screenshots.png "App screenshots")

## How to contribute
If you want to [contribute](https://nextcloud.com/contribute/) to Nextcloud, you are very welcome: 

- on our IRC channels [![irc](https://img.shields.io/badge/IRC-%23nextcloud%20on%20freenode-orange.svg)](https://webchat.freenode.net/?channels=nextcloud) and [![irc](https://img.shields.io/badge/IRC-%23nextcloud--mobile%20on%20freenode-blue.svg)](https://webchat.freenode.net/?channels=nextcloud-mobile) on freenode
- our forum at https://help.nextcloud.com
- for translations of the app on [Transifex](https://www.transifex.com/nextcloud/nextcloud/android/)
- opening issues and PRs (including a corresponding issue)

## Contribution Guidelines & License

[GPLv2](https://github.com/nextcloud/android/blob/master/LICENSE.txt). All contributions to this repository from June, 16 2016 on are considered to be licensed under the AGPLv3 or any later version.

Nextcloud doesn't require a CLA (Contributor License Agreement). The copyright belongs to all the individual contributors. Therefore we recommend that every contributor adds following line to the header of a file, if they changed it substantially:

```
@copyright Copyright (c) <year>, <your name> (<your email address>)
```

Please read the [Code of Conduct](https://nextcloud.com/community/code-of-conduct/). This document offers some guidance to ensure Nextcloud participants can cooperate effectively in a positive and inspiring atmosphere, and to explain how together we can strengthen and support each other.

Please review the [guidelines for contributing](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md) to this repository.

More information how to contribute: [https://nextcloud.com/contribute/](https://nextcloud.com/contribute/)

## Start contributing
Make sure you read [SETUP.md](https://github.com/nextcloud/android/blob/master/SETUP.md) and [CONTRIBUTING.md](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md) before you start working on this project. But basically: fork this repository and contribute back using pull requests to the master branch.
Easy starting points are also reviewing [pull requests](https://github.com/nextcloud/android/pulls) and working on [starter issues](https://github.com/nextcloud/android/issues?q=is%3Aopen+is%3Aissue+label%3A%22starter+issue%22).

### Getting debug info via logcat
#### With a computer:
- connect the device via USB
- open command prompt/terminal
- enter `adb logcat | grep "$(adb shell ps | awk '/com.nextcloud.client/{print $2}')" > logcatOutput.txt` to save the output to this file

**Note:** You must have [adb](https://developer.android.com/studio/releases/platform-tools.html) installed first!

#### On a device (with root)
- open terminal app *(can be enabled in developer options)*
- get root access via "su"
- enter `logcat -d -f /sdcard/logcatOutput.txt`
- you will have to filter the output manually, as above approach is not working on device

or 

- use [CatLog](https://play.google.com/store/apps/details?id=com.nolanlawson.logcat) or [aLogcat](https://play.google.com/store/apps/details?id=org.jtb.alogcat)

**Note:** Your device needs to be rooted for this approach!

## Development version
- [APK (direct download)](https://download.nextcloud.com/android/dev/latest.apk)
- [F-Droid](https://f-droid.org/repository/browse/?fdfilter=nextcloud&fdid=com.nextcloud.android.beta)

## Support

If you need assistance or want to ask a question about the Android app, you are welcome to [ask for support](https://help.nextcloud.com/c/clients/android) in our Forums or the [IRC-Channel](https://webchat.freenode.net/?channels=nextcloud-mobile). If you have found a bug, feel free to [open a new Issue on GitHub](https://github.com/nextcloud/android/issues). Keep in mind, that this repository only manages the Android app. If you find bugs or have problems with the server/backend, you should ask the [Nextcloud server team](https://github.com/nextcloud/server) for help!

## Remarks

Google Play and the Google Play logo are trademarks of Google Inc.
