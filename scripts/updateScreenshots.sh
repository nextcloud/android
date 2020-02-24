#!/usr/bin/env bash

if ( [[ $(grep NC_TEST_SERVER_BASEURL ~/.gradle/gradle.properties   | grep -v "#" -c) -gt 0 ]] ); then
    echo "This will not use server in docker. Please comment in .gradle/gradle.properties. Aborting!"
    exit 1
fi

## emulator
if ( [[ $(emulator -list-avds | grep uiComparison -c) -eq 0 ]] ); then
    avdmanager delete avd -n uiComparison
    (sleep 5; echo "no") | avdmanager create avd -n uiComparison -c 100M -k "system-images;android-27;google_apis;x86" --abi "google_apis/x86"
fi

emulator -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-window -no-audio -skin 500x833 1>/dev/null &
PID=$(echo $!)

## server
docker run --name=uiComparison nextcloudci/server:server-17 1>/dev/null &
sleep 5
IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' uiComparison)

if [[ $IP = "" ]]; then
    echo "no server"
    exit 1
fi

## run on server
cp gradle.properties gradle.properties_
sed -i s"/server/$IP/" gradle.properties
scripts/wait_for_emulator.sh

# setup test server
docker exec uiComparison /bin/sh -c "/usr/local/bin/initnc.sh"
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user1 php /var/www/html/occ user:add --password-from-env --display-name='User One' user1\""
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user2 php /var/www/html/occ user:add --password-from-env --display-name='User Two' user2\""
docker exec uiComparison /bin/sh -c "su www-data -c \"OC_PASS=user3 php /var/www/html/occ user:add --password-from-env --display-name='User Three' user3\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ user:setting user2 files quota 1G\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:add users\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:adduser users user1\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ group:adduser users user2\""
docker exec uiComparison /bin/sh -c "su www-data -c \"git clone -b master https://github.com/nextcloud/text.git /var/www/html/apps/text/\""
docker exec uiComparison /bin/sh -c "su www-data -c \"php /var/www/html/occ app:enable text\""
docker exec uiComparison /bin/sh -c "/usr/local/bin/run.sh"

## update/create all screenshots
./gradlew executeScreenshotTests -Precord

## update screenshots in a class
#./gradlew executeScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest

## update single screenshot within a class
#./gradlew executeScreenshotTests \
#-Precord \
#-Pandroid.testInstrumentationRunnerArguments.class=\
#com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragmentTest#showNotEnoughSpaceDialogForFile

mv gradle.properties_ gradle.properties

# tidy up
kill $PID
docker stop uiComparison
docker rm uiComparison
