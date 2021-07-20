#!/usr/bin/env bash

if [[ $(grep NC_TEST_SERVER_BASEURL ~/.gradle/gradle.properties   | grep -v "#" -c) -gt 0 ]]; then
    echo "This will not use server in docker. Please comment in .gradle/gradle.properties. Aborting!"
    exit 1
fi

## emulator
if [[ ! $(emulator -list-avds | grep uiComparison -c) -eq 0 ]]; then
    avdmanager delete avd -n uiComparison
    (sleep 5; echo "no") | avdmanager create avd -n uiComparison -c 100M -k "system-images;android-27;google_apis;x86" --abi "google_apis/x86"
fi

if [ "$1" == "debug" ]; then
  emulator -writable-system -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-audio -skin 500x833 1>/dev/null &
else
  emulator -writable-system -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-window -no-audio -skin 500x833 1>/dev/null &
fi
PID=$!

## server
docker run --name=uiComparison nextcloudci/server --entrypoint '/usr/local/bin/initnc.sh' 1>/dev/null &
sleep 5
IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' uiComparison)

if [[ $IP = "" ]]; then
    echo "no server"
    exit 1
fi

## wait for server to finish
scripts/wait_for_server.sh "$IP"

# setup test server
docker exec uiComparison /bin/sh -c "echo $IP server >> /etc/hosts"
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user1 php /var/www/html/occ user:add --password-from-env --display-name='User One' user1\""
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user2 php /var/www/html/occ user:add --password-from-env --display-name='User Two' user2\""
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user3 php /var/www/html/occ user:add --password-from-env --display-name='User Three' user3\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ user:setting user2 files quota 1G\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:add users\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:adduser users user1\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:adduser users user2\""
docker exec uiComparison /bin/sh -c "su www-data -c \"git clone -b master https://github.com/nextcloud/activity.git /var/www/html/apps/activity/\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ app:enable activity\""
docker exec uiComparison /bin/sh -c "su www-data -c \"git clone -b master https://github.com/nextcloud/text.git /var/www/html/apps/text/\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ app:enable text\""
docker exec uiComparison /bin/sh -c "su www-data -c \"git clone -b master https://github.com/nextcloud/end_to_end_encryption/  /var/www/html/apps/end_to_end_encryption/\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ app:enable end_to_end_encryption\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"git clone -b master https://github.com/nextcloud/circles.git /var/www/html/apps/circles/\""
#docker exec uiComparison /bin/sh -c "apt-get update; apt-get -y install composer"
#docker exec uiComparison /bin/sh -c "su www-data -c \"cd /var/www/html/apps/circles; composer install\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ app:enable -f circles\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ config:app:set circles --value 1 allow_non_ssl_links\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ config:app:set circles --value 1 local_is_non_ssl\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ config:system:set allow_local_remote_servers --value true --type bool\""
#docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ circles:manage:create test public publicCircle\""
docker exec uiComparison /bin/sh -c "/usr/local/bin/run.sh"

## wait for server to finish
scripts/wait_for_server.sh "$IP"

scripts/wait_for_emulator.sh

# change server to ip on emulator
adb root
sleep 2
adb remount
sleep 2
adb shell "mount -o remount,rw /system"
sleep 2
adb shell "echo $IP server >> /system/etc/hosts"

sed -i s'#<bool name="is_beta">false</bool>#<bool name="is_beta">true</bool>#'g src/main/res/values/setup.xml

## update/create all screenshots
#./gradlew gplayDebugExecuteScreenshotTests -Precord \
#-Pandroid.testInstrumentationRunnerArguments.annotation=com.owncloud.android.utils.ScreenshotTest

## update screenshots in a class
#./gradlew gplayDebugExecuteScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest

## update single screenshot within a class
#./gradlew gplayDebugExecuteScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.nextcloud.client.FileDisplayActivityIT#showShares

resultCode=-1
retryCount=0
until [ $resultCode -eq 0 ] || [ $retryCount -gt 2 ]
do
  # test all screenshots
  ./gradlew gplayDebugExecuteScreenshotTests \
  -Pandroid.testInstrumentationRunnerArguments.annotation=com.owncloud.android.utils.ScreenshotTest

resultCode=$?
((retryCount++))
done

sed -i s'#<bool name="is_beta">true</bool>#<bool name="is_beta">false</bool>#'g src/main/res/values/setup.xml

if [ "$1" == "debug" ]; then
  exit
fi

# tidy up
kill "$PID"
docker stop uiComparison
docker rm uiComparison
