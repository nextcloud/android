<!--
 ~ SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
 ~ SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
-->
# [Nextcloud](https://nextcloud.com) Android app :iphone:

[![REUSE status](https://api.reuse.software/badge/github.com/nextcloud/android)](https://api.reuse.software/info/github.com/nextcloud/android) [![Build Status](https://drone.nextcloud.com/api/badges/nextcloud/android/status.svg)](https://drone.nextcloud.com/nextcloud/android) [![Codacy Badge](https://app.codacy.com/project/badge/Grade/fb4cf26336774ee3a5c9adfe829c41aa)](https://app.codacy.com/gh/nextcloud/android/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade) [![Releases](https://img.shields.io/github/release/nextcloud/android.svg)](https://github.com/nextcloud/android/releases/latest)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" 
alt="Download from Google Play" 
height="80">](https://play.google.com/store/apps/details?id=com.nextcloud.client)
[<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.nextcloud.client/)

Signing certificate fingerprint to [verify](https://developer.android.com/studio/command-line/apksigner#usage-verify) the APK:
- APK with "gplay" name, found [here](https://github.com/nextcloud/android/releases) or distributed via Google Play Store
- APK with "nextcloud", found [here](https://github.com/nextcloud/android/releases)
- not suitable for Fdroid downloads, as Fdroid is signing it on their own
```
SHA-256: fb009522f65e25802261b67b10a45fd70e610031976f40b28a649e152ded0373   
SHA-1: 74aa1702e714941be481e1f7ce4a8f779c19dcea
```

**The Android client for [Nextcloud](https://nextcloud.com). Easily work with your data on your Nextcloud.**

![App screenshots](/doc/Nextcloud_Android_Screenshots.png "App screenshots")

## Getting help :rescue\_worker\_helmet:

Note: The section *Known Problems / FAQs* below may already document your situation.

If you need assistance or want to ask a question about the Android app, you are welcome to [ask for support](https://help.nextcloud.com/c/clients/android) in the [Nextcloud Help Forum](https://help.nextcloud.com). If you have found a probable bug or have an enhancement idea, feel free to [open a new Issue on GitHub](https://github.com/nextcloud/android/issues).

If you're not sure if something is a bug or a configuration matter (with your client, server, proxy, etc.), the [Nextcloud Help Forum](https://help.nextcloud.com) is probably the best place to start so that you can get feedback (you can always return here, after getting feedback there, to report a suspected bug). 

Keep in mind, that this repository only manages the Android app. If you find bugs or have problems with the server/backend, you should use the Nextcloud Help Forum to ask for help or report the bug to the [Nextcloud server team](https://github.com/nextcloud/server)!

## How to contribute :rocket:

If you want to [contribute](https://nextcloud.com/contribute/) to the Nextcloud Android client app, there are many ways to help whether or not you are a coder: 

*   helping out other users on our forum at https://help.nextcloud.com
*   providing translations of the app on [Transifex](https://app.transifex.com/nextcloud/nextcloud/android/)
*   reporting problems / suggesting enhancements by [opening new issues](https://github.com/nextcloud/android/issues/new/choose)
*   implementing proposed bug fixes and enhancement ideas by submitting PRs (associated with a corresponding issue preferably)
*   reviewing [pull requests](https://github.com/nextcloud/android/pulls) and providing feedback on code, implementation, and functionality
*   installing and testing [pull request builds](https://github.com/nextcloud/android/pulls), [daily/dev builds](https://github.com/nextcloud/android#development-version-hammer), or [RCs/release candidate builds](https://github.com/nextcloud/android/releases) 
*   enhancing Admin, User, or Developer [documentation](https://github.com/nextcloud/documentation/)
*   hitting hard on the latest stable release by testing fundamental features and evaluating the user experience
*   proactively getting familiar with [how to gather debug logs](https://github.com/nextcloud/android#getting-debug-info-via-logcat-mag) from your devices (so that you are prepared to provide a detailed report if you encounter a problem with the app in the future)

## Contribution Guidelines & License :scroll:

[GPLv2](https://github.com/nextcloud/android/blob/master/LICENSE.txt). All contributions to this repository from June, 16 2016 on are considered to be licensed under the AGPLv3 or any later version.

Nextcloud doesn't require a CLA (Contributor License Agreement). The copyright belongs to all the individual contributors. Therefore we recommend that every contributor adds following line to the header of a file, if they changed it substantially:

	SPDX-FileCopyrightText: <year> <your name> <your email address>

Please read the [Code of Conduct](https://nextcloud.com/community/code-of-conduct/). This document offers some guidance to ensure Nextcloud participants can cooperate effectively in a positive and inspiring atmosphere, and to explain how together we can strengthen and support each other.

Please review the [guidelines for contributing](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md) to this repository.

More information on how to contribute: <https://nextcloud.com/contribute/>

## Start contributing :hammer\_and\_wrench:

Make sure you read [SETUP.md](https://github.com/nextcloud/android/blob/master/SETUP.md) and [CONTRIBUTING.md](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md) before you start working on this project. But basically: fork this repository and contribute back using pull requests to the master branch.
Easy starting points are also reviewing [pull requests](https://github.com/nextcloud/android/pulls) and working on [starter issues](https://github.com/nextcloud/android/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22).

## Logs

### Getting debug info via logcat :mag:

#### With a linux computer:

*   enable USB-Debugging in your smartphones developer settings and connect it via USB
*   open command prompt/terminal
*   enter `adb logcat --pid=$(adb shell pidof -s 'com.nextcloud.client') > logcatOutput.txt` to save the output to this file

**Note:** You must have [adb](https://developer.android.com/studio/releases/platform-tools.html) installed first!

#### On Windows:

*   download and install [Minimal ADB and fastboot](https://forum.xda-developers.com/t/tool-minimal-adb-and-fastboot-2-9-18.2317790/#post-42407269)
*   enable USB-Debugging in your smartphones developer settings and connect it via USB
*   launch Minimal ADB and fastboot
*   enter `adb shell pidof -s 'com.nextcloud.client'` and use the output as `<processID>` in the following command:
*   `adb logcat --pid=<processID> > "%USERPROFILE%\Downloads\logcatOutput.txt"` (This will produce a `logcatOutput.txt` file in your downloads)
*   if the processID is `18841`, an example command is: `adb logcat --pid=18841 > "%USERPROFILE%\Downloads\logcatOutput.txt"` (You might cancel the process after a while manually: it will not be exited automatically.)
*   For a PowerShell terminal, replace `%USERPROFILE%` with `$env:USERPROFILE` in the commands above.

#### On a device (with root) :wrench:

*   open terminal app *(can be enabled in developer options)*
*   get root access via "su"
*   enter `logcat -d --pid $(pidof -s com.nextcloud.client) -f /sdcard/logcatOutput.txt`

or

*   use [CatLog](https://play.google.com/store/apps/details?id=com.nolanlawson.logcat) or [aLogcat](https://play.google.com/store/apps/details?id=org.jtb.alogcat)

**Note:** Your device needs to be rooted for this approach!

## Development version :hammer:

*   [APK (direct download)](https://download.nextcloud.com/android/dev/latest.apk)
*   [F-Droid](https://f-droid.org/en/packages/com.nextcloud.android.beta/)

## Known Problems and FAQs

### Push notifications do not work on F-Droid editions

Push Notifications are not currently supported in the F-Droid builds due to dependencies on Google Play services.

## Remarks :scroll:

Google Play and the Google Play logo are trademarks of Google Inc.
