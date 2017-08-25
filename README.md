# [Nextcloud](https://nextcloud.com) Android app [![Build Status](https://drone.nextcloud.com/api/badges/nextcloud/android/status.svg)](https://drone.nextcloud.com/nextcloud/android) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/80401cb343854343b4d94acbfb72d3ec)](https://www.codacy.com/app/Nextcloud/android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=nextcloud/android&amp;utm_campaign=Badge_Grade) [![Releases](https://img.shields.io/github/release/nextcloud/android.svg)](https://github.com/nextcloud/android/releases/latest)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" 
      alt="Download from Google Play" 
      height="80">](https://play.google.com/store/apps/details?id=com.nextcloud.client)
[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.nextcloud.client/)

[![irc](https://img.shields.io/badge/IRC-%23nextcloud%20on%20freenode-orange.svg)](https://webchat.freenode.net/?channels=nextcloud)
[![irc](https://img.shields.io/badge/IRC-%23nextcloud--mobile%20on%20freenode-blue.svg)](https://webchat.freenode.net/?channels=nextcloud-mobile)

Please stay tuned while we get all the repositories up.

Meanwhile check out https://nextcloud.com and follow us on https://twitter.com/nextclouders

If you want to [contribute](https://nextcloud.com/contribute/), you are very welcome: 

- on our IRC channels #nextcloud & #nextcloud-mobile irc://#nextcloud-mobile@freenode.net (on freenode) and 
- our forum at https://help.nextcloud.com

Please read the [Code of Conduct](https://nextcloud.com/community/code-of-conduct/). This document offers some guidance to ensure Nextcloud participants can cooperate effectively in a positive and inspiring atmosphere, and to explain how together we can strengthen and support each other.

if you want to join the Github organization just let us know and we’ll add you! :)

*This is by the community, for the community. Everyone is welcome! :)*

## Start contributing
Make sure you read [SETUP.md](https://github.com/nextcloud/android/blob/master/SETUP.md) and [CONTRIBUTING.md](https://github.com/nextcloud/android/blob/master/CONTRIBUTING.md) when you start working on this project. Basically: Fork this repository and contribute back using pull requests to the master branch.
Easy starting points are also reviewing [pull requests](https://github.com/nextcloud/android/pulls) and working on [starter issue](https://github.com/nextcloud/android/issues?q=is%3Aopen+is%3Aissue+label%3A%22starter+issue%22).

### Get debug infos via logcat
#### With computer
- connect device via usb
- open command prompt/terminal
- adb logcat > logcatOutput.txt to save the output to this file

**Note:** you must have [adb](https://developer.android.com/studio/releases/platform-tools.html) installed first

#### On device (with root)
- open terminal app (can be enabled in developer options)
- get root access via "su"
- enter "logcat -d -f /sdcard/logcatOutput.txt"

or 

- use [CatLog](https://play.google.com/store/apps/details?id=com.nolanlawson.logcat) or [aLogcat](https://play.google.com/store/apps/details?id=org.jtb.alogcat)

**Note:** Your device needs to be rooted for this approach.

**Dev version:** [direct download](https://download.nextcloud.com/android/dev/latest.apk) or via [Fdroid](https://f-droid.org/repository/browse/?fdfilter=nextcloud&fdid=com.nextcloud.android.beta)

**License:** [GPLv2](https://github.com/nextcloud/android/blob/master/LICENSE.txt)

New contributions are added under [AGPLv3](https://www.gnu.org/licenses/agpl.txt).

Google Play and the Google Play logo are trademarks of Google Inc.
