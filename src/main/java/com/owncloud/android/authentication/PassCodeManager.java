/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.PassCodeActivity;
import com.owncloud.android.ui.activity.Preferences;
import com.owncloud.android.ui.activity.RequestCredentialsActivity;
import com.owncloud.android.utils.DeviceCredentialUtils;

import java.util.HashSet;
import java.util.Set;

public class PassCodeManager {

    private static final Set<Class> exemptOfPasscodeActivities;

    public static final int PASSCODE_ACTIVITY = 9999;

    static {
        exemptOfPasscodeActivities = new HashSet<Class>();
        exemptOfPasscodeActivities.add(PassCodeActivity.class);
        exemptOfPasscodeActivities.add(RequestCredentialsActivity.class);
        // other activities may be exempted, if needed
    }

    private static final int PASS_CODE_TIMEOUT = 1000;
        // keeping a "low" positive value is the easiest way to prevent the pass code is requested on rotations

    private static PassCodeManager passCodeManagerInstance = null;

    private Long timestamp = 0L;
    private int visibleActivitiesCounter = 0;

    public static PassCodeManager getPassCodeManager() {
        if (passCodeManagerInstance == null) {
            passCodeManagerInstance = new PassCodeManager();
        }
        return passCodeManagerInstance;
    }

    protected PassCodeManager() {}

    public void onActivityCreated(Activity activity) {
        if (passCodeIsEnabled() || deviceCredentialsAreEnabled(activity)) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public void onActivityStarted(Activity activity) {
        if (!exemptOfPasscodeActivities.contains(activity.getClass()) && passCodeShouldBeRequested()) {

            Intent i = new Intent(MainApp.getAppContext(), PassCodeActivity.class);
            i.setAction(PassCodeActivity.ACTION_CHECK);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivityForResult(i, PASSCODE_ACTIVITY);
        }


        if (!sExemptOfPasscodeActivites.contains(activity.getClass()) && Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.M && deviceCredentialsShouldBeRequested(activity) &&
                !DeviceCredentialUtils.tryEncrypt(activity)) {
            Intent i = new Intent(MainApp.getAppContext(), RequestCredentialsActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivityForResult(i, PASSCODE_ACTIVITY);
        }

        visibleActivitiesCounter++;    // keep it AFTER passCodeShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (visibleActivitiesCounter > 0) {
            visibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if ((passCodeIsEnabled() || deviceCredentialsAreEnabled(activity)) && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private void setUnlockTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    private boolean passCodeShouldBeRequested() {
        return (passCodeIsEnabled() && hasAuthenticationTimeoutExpired());
    }

    private boolean passCodeIsEnabled() {
        SharedPreferences appPrefs = PreferenceManager
                .getDefaultSharedPreferences(MainApp.getAppContext());
        return (appPrefs.getString(Preferences.PREFERENCE_LOCK, "")
                .equals(Preferences.LOCK_PASSCODE));
    }

    private boolean deviceCredentialsShouldBeRequested(Activity activity) {
        if ((System.currentTimeMillis() - mTimestamp) > PASS_CODE_TIMEOUT && visibleActivitiesCounter <= 0) {
            return deviceCredentialsAreEnabled(activity);
        }
        return false;
    }

    private boolean hasAuthenticationTimeoutExpired() {
        return (System.currentTimeMillis() - timestamp) > PASS_CODE_TIMEOUT && visibleActivitiesCounter <= 0;
    }

    private boolean fingerprintIsEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                appPrefs.getBoolean(Preferences.PREFERENCE_USE_FINGERPRINT, false);
    }

    private boolean deviceCredentialsAreEnabled(Activity activity) {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        return appPrefs.getString(Preferences.PREFERENCE_LOCK, "").equals(Preferences.LOCK_DEVICE_CREDENTIALS);
    }
}
