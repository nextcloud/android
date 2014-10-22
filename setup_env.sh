#!/bin/bash -e

git submodule init
git submodule update
android update project -p actionbarsherlock/library -n ActionBarSherlock
android update lib-project -p owncloud-android-library
android update project -p .
android update project -p oc_jb_workaround
cp third_party/android-support-library/android-support-v4.jar actionbarsherlock/library/libs/android-support-v4.jar 
android update test-project -p tests -m ..
