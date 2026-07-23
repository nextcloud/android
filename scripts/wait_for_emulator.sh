#!/bin/bash

#
# SPDX-FileCopyrightText: 2021 Ralf Kistner <ralf@embarkmobile.com>
# SPDX-License-Identifier: CC0-1.0
#

# Originally written by Ralf Kistner <ralf@embarkmobile.com>, but placed in the public domain

bootanim=""
failcounter=0
checkcounter=0

until [[ "$bootanim" =~ "stopped" ]]; do
   bootanim=`adb -e shell getprop init.svc.bootanim 2>&1`
   echo "($checkcounter) $bootanim"
   if [[ "$bootanim" =~ "not found" || "$bootanim" =~ "error" ]]; then
      let "failcounter += 1"
      if [[ $failcounter -gt 3 ]]; then
        echo "Failed to start emulator"
        exit 1
      fi
   fi
   let "checkcounter += 1"
   sleep 5
done
echo "($checkcounter) Done"

# The bootanim service can stop before the system is fully booted; wait for it so the
# settings below actually stick.
until [[ "$(adb -e shell getprop sys.boot_completed 2>&1 | tr -d '[:space:]')" == "1" ]]; do
   sleep 2
done

# Keep the screen awake and unlocked for the whole run. On a headless emulator the
# screen can sleep or keep the keyguard up, which leaves the app window without focus
# and makes Espresso fail with RootViewWithoutFocusException.
# stayon only prevents future sleep, so we also wake the screen and drop the keyguard.
adb -e shell settings put system screen_off_timeout 2147483647
adb -e shell svc power stayon true
adb -e shell input keyevent 224          # KEYCODE_WAKEUP: turn a sleeping screen back on
adb -e shell locksettings set-disabled true
adb -e shell wm dismiss-keyguard
adb -e shell input keyevent 82

# Disable animations so Espresso does not wait on "not request layout".
adb -e shell settings put global window_animation_scale 0.0
adb -e shell settings put global transition_animation_scale 0.0
adb -e shell settings put global animator_duration_scale 0.0

echo "($checkcounter) Unlocked emulator screen"
