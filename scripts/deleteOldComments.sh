#!/usr/bin/env bash
#1: LOG_USERNAME
#2: LOG_PASSWORD
#3: DRONE_BUILD_NUMBER
#4: BRANCH (stable or master)
#5: TYPE (IT or Unit)
#6: DRONE_PULL_REQUEST
#7: GITHUB_TOKEN

BRANCH=$1
TYPE=$2
PR=$3
GITHUB_USER=$4
GITHUB_PASSWORD=$5
BRANCH_TYPE=$BRANCH-$TYPE

 # delete all old comments, matching this type
echo "Deleting old comments for $BRANCH_TYPE"
oldComments=$(curl 2>/dev/null --header "authorization: Bearer $GITHUB_TOKEN" -X GET https://api.github.com/repos/nextcloud/android/issues/$PR/comments | jq --arg TYPE $BRANCH_TYPE '.[] | (.id |tostring) + "|" + (.user.login | test("nextcloud-android-bot") | tostring) + "|" + (.body | test([$TYPE]) | tostring)'| grep "true|true" | tr -d "\"" | cut -f1 -d"|")
count=$(echo $oldComments | grep true | wc -l)
echo "Found $count old comments"

echo $oldComments | while read comment ; do
echo "Deleting comment: $comment"
curl 2>/dev/null --header "authorization: Bearer $GITHUB_TOKEN" -X DELETE https://api.github.com/repos/nextcloud/android/issues/comments/$comment
done

exit 0
