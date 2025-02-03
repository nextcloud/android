#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

BRANCH=$1
LOG_USERNAME=$2
LOG_PASSWORD=$3
BUILD_NUMBER=$4
PR_NUMBER=$5


stableBranch="stable-3.30"
repository="android"

ruby scripts/analysis/lint-up.rb
lintValue=$?

curl "https://www.kaminsky.me/nc-dev/$repository-findbugs/$stableBranch.xml" -o "/tmp/$stableBranch.xml"
[[ ! -e "/tmp/$stableBranch.xml" ]] && exit 1

ruby scripts/analysis/spotbugs-up.rb "$stableBranch"
spotbugsValue=$?

# exit codes:
# 0: count was reduced
# 1: count was increased
# 2: count stayed the same

source scripts/lib.sh

echo "Branch: $BRANCH"

if [ "$BRANCH" = $stableBranch ]; then
    echo "New spotbugs result for $stableBranch at: https://www.kaminsky.me/nc-dev/$repository-findbugs/$stableBranch.html"
    curl -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT https://nextcloud.kaminsky.me/remote.php/dav/files/${LOG_USERNAME}/$repository-findbugs/$stableBranch.html --upload-file app/build/reports/spotbugs/spotbugs.html
    curl 2>/dev/null -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT "https://nextcloud.kaminsky.me/remote.php/dav/files/${LOG_USERNAME}/$repository-findbugs/$stableBranch.xml" --upload-file app/build/reports/spotbugs/gplayDebug.xml

    if [ $lintValue -ne 1 ]; then
        echo "New lint result for $stableBranch at: https://www.kaminsky.me/nc-dev/$repository-lint/$stableBranch.html"
        curl -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT https://nextcloud.kaminsky.me/remote.php/dav/files/${LOG_USERNAME}/$repository-lint/$stableBranch.html --upload-file app/build/reports/lint/lint.html
        exit 0
    fi
else
    if [ -e "${BUILD_NUMBER}" ]; then
        6=$stableBranch"-"$(date +%F)
    fi
    echo "New lint results at https://www.kaminsky.me/nc-dev/$repository-lint/${BUILD_NUMBER}.html"
    curl 2>/dev/null -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT "https://nextcloud.kaminsky.me/remote.php/dav/files/${LOG_USERNAME}/$repository-lint/${BUILD_NUMBER}.html" --upload-file app/build/reports/lint/lint.html

    echo "New spotbugs results at https://www.kaminsky.me/nc-dev/$repository-findbugs/${BUILD_NUMBER}.html"
    curl 2>/dev/null -u "${LOG_USERNAME}:${LOG_PASSWORD}" -X PUT "https://nextcloud.kaminsky.me/remote.php/dav/files/${LOG_USERNAME}/$repository-findbugs/${BUILD_NUMBER}.html" --upload-file app/build/reports/spotbugs/spotbugs.html

    # delete all old comments, starting with Codacy
    oldComments=$(curl_gh -X GET "https://api.github.com/repos/nextcloud/$repository/issues/${PR_NUMBER}/comments" | jq '.[] | select((.user.login | contains("github-actions")) and  (.body | test("<h1>Codacy.*"))) | .id')

    echo "$oldComments" | while read -r comment ; do
        curl_gh -X DELETE "https://api.github.com/repos/nextcloud/$repository/issues/comments/$comment"
    done

    # lint and spotbugs file must exist
    if [ ! -s app/build/reports/lint/lint.html ] ; then
        echo "lint.html file is missing!"
        exit 1
    fi

    if [ ! -s app/build/reports/spotbugs/spotbugs.html ] ; then
        echo "spotbugs.html file is missing!"
        exit 1
    fi

    # add comment with results
    lintResultNew=$(grep "Lint Report.* [0-9]* warning" app/build/reports/lint/lint.html | cut -f2 -d':' |cut -f1 -d'<')

    lintErrorNew=$(echo $lintResultNew | grep "[0-9]* error" -o | cut -f1 -d" ")
    if ( [ -z $lintErrorNew ] ); then
        lintErrorNew=0
    fi

    lintWarningNew=$(echo $lintResultNew | grep "[0-9]* warning" -o | cut -f1 -d" ")
    if ( [ -z $lintWarningNew ] ); then
        lintWarningNew=0
    fi

    lintResultOld=$(curl 2>/dev/null "https://raw.githubusercontent.com/nextcloud/$repository/$stableBranch/scripts/analysis/lint-results.txt")
    lintErrorOld=$(echo $lintResultOld | grep "[0-9]* error" -o | cut -f1 -d" ")
    if ( [ -z $lintErrorOld ] ); then
        lintErrorOld=0
    fi

    lintWarningOld=$(echo $lintResultOld | grep "[0-9]* warning" -o | cut -f1 -d" ")
    if ( [ -z $lintWarningOld ] ); then
        lintWarningOld=0
    fi

    if [ $stableBranch = "master" ] ; then
        codacyValue=$(curl 2>/dev/null https://app.codacy.com/dashboards/breakdown\?projectId\=44248 | grep "total issues" | cut -d">" -f3 | cut -d"<" -f1)
        codacyResult="<h1>Codacy</h1>$codacyValue"
    else
        codacyResult=""
    fi

    lintResult="<h1>Lint</h1><table width='500' cellpadding='5' cellspacing='2'><tr class='tablerow0'><td>Type</td><td><a href='https://www.kaminsky.me/nc-dev/$repository-lint/$stableBranch.html'>$stableBranch</a></td><td><a href='https://www.kaminsky.me/nc-dev/$repository-lint/${BUILD_NUMBER}.html'>PR</a></td></tr><tr class='tablerow1'><td>Warnings</td><td>$lintWarningOld</td><td>$lintWarningNew</td></tr><tr class='tablerow0'><td>Errors</td><td>$lintErrorOld</td><td>$lintErrorNew</td></tr></table>"

    spotbugsResult="<h1>SpotBugs</h1>$(scripts/analysis/spotbugsComparison.py "/tmp/$stableBranch.xml" app/build/reports/spotbugs/gplayDebug.xml --link-new "https://www.kaminsky.me/nc-dev/$repository-findbugs/${BUILD_NUMBER}.html" --link-base "https://www.kaminsky.me/nc-dev/$repository-findbugs/$stableBranch.html")"

    if ( [ $lintValue -eq 1 ] ) ; then
        lintMessage="<h1>Lint increased!</h1>"
    fi

    if ( [ $spotbugsValue -eq 1 ] ) ; then
        spotbugsMessage="<h1>SpotBugs increased!</h1>"
    fi

    # check gplay limitation: all changelog files must only have 500 chars
    gplayLimitation=$(scripts/checkGplayLimitation.sh)

    if [ ! -z "$gplayLimitation" ]; then
        gplayLimitation="<h1>Following files are beyond 500 char limit:</h1><br><br>"$gplayLimitation
    fi

    # check for NotNull
    if [[ $(grep org.jetbrains.annotations app/src/main/* -irl | wc -l) -gt 0 ]] ; then
        notNull="org.jetbrains.annotations.* is used. Please use androidx.annotation.* instead.<br><br>"
    fi

    bodyContent="$codacyResult $lintResult $spotbugsResult $lintMessage $spotbugsMessage $gplayLimitation $notNull"
    echo "$bodyContent" >> "$GITHUB_STEP_SUMMARY"
    payload="{ \"body\" : \"$bodyContent\" }"
    curl_gh -X POST "https://api.github.com/repos/nextcloud/$repository/issues/${PR_NUMBER}/comments" -d "$payload"

    if [ ! -z "$gplayLimitation" ]; then
        exit 1
    fi

    if [ ! $lintValue -eq 2 ]; then
        exit $lintValue
    fi

    if [ -n "$notNull" ]; then
        exit 1
    fi

    if [ $spotbugsValue -eq 2 ]; then
        exit 0
    else
        exit $spotbugsValue
    fi
fi
