#!/bin/bash

cd ci/android/

for i in {1..10}; do
    docker-compose exec -T android adb devices
    [[ $? -eq 0 ]] && break

    docker-compose logs

    [[ $i -eq 10 ]] && exit 1 # failed to wait for emulator

    sleep 30
done

cd ../../scripts/
./wait_for_emulator.sh


