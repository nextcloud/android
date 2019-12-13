#!/usr/bin/env bash

## emulator
if ( [[ $(emulator -list-avds | grep uiComparison -c) -eq 0 ]] ); then
    (sleep 5; echo "no") | avdmanager create avd -n uiComparison -c 100M -k "system-images;android-27;google_apis;x86" --abi "google_apis/x86"
fi

emulator -avd uiComparison -no-snapshot -gpu swiftshader_indirect -no-window -no-audio -skin 500x833 1>/dev/null &
PID=$(echo $!)

## server
docker run --name=uiComparison nextcloudci/server:server-3 1>/dev/null &
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
scripts/wait_for_server.sh ${IP}
./gradlew executeScreenshotTests -Precord
mv gradle.properties_ gradle.properties

# tidy up
kill $PID
docker stop uiComparison
docker rm uiComparison
