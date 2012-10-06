#!/bin/bash

git submodule init
git submodule update
android update project -p actionbarsherlock/library
android update project -p .
cp third_party/android-support-library/android-support-v4.jar actionbarsherlock/library/libs/android-support-v4.jar 
cd tests
android update test-project -m .. -p .
