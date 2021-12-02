#!/usr/bin/env bash
#1: BRANCH
#2: TYPE
#3: PR
#4: GITHUB_TOKEN

BRANCH=$1
TYPE=$2
PR=$3
GITHUB_TOKEN=$4

BRANCH_TYPE=$BRANCH-$TYPE

 # delete all old comments, matching this type
echo "Deleting old comments for $BRANCH_TYPE"
oldComments=$(curl 2>/dev/null --header "authorization: Bearer $GITHUB_TOKEN" -X GET https://api.github.com/repos/nextcloud/android/issues/$PR/comments | jq --arg TYPE $BRANCH_TYPE '.[] | (.id |tostring) + "|" + (.user.login | test("(nextcloud-android-bot|github-actions)") | tostring) + "|" + (.body | test([$TYPE]) | tostring)'| grep "true|true" | tr -d "\"" | cut -f1 -d"|")
count=$(echo -n "$oldComments" | grep -c '^')
echo "Found $count old comments"

if [ "$count" -gt 0 ]; then
  echo "$oldComments" | while read comment ; do
    echo "Deleting comment: $comment"
    curl 2>/dev/null --header "authorization: Bearer $GITHUB_TOKEN" -X DELETE https://api.github.com/repos/nextcloud/android/issues/comments/$comment
  done
fi

exit 0
