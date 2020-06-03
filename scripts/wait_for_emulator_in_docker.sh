#!/bin/bash

cd ci/android/

for i in {1..10}; do
    docker-compose exec -T android adb devices
    [[ $? -eq 0 ]] && break
    sleep 20
    [[ $i -eq 10 ]] && exit 1 # failed to wait for emulator
done

cd ../../scripts/
./wait_for_emulator.sh


