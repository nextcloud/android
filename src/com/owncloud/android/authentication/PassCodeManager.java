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
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.PassCodeActivity;

import java.util.HashSet;
import java.util.Set;

public class PassCodeManager {

    private static final Set<Class> sExemptOfPasscodeActivites;

    static {
        sExemptOfPasscodeActivites = new HashSet<Class>();
        sExemptOfPasscodeActivites.add(PassCodeActivity.class);
        // other activities may be exempted, if needed
    }

    private static int PASS_CODE_TIMEOUT = 1000;
        // keeping a "low" positive value is the easiest way to prevent the pass code is requested on rotations

    public static PassCodeManager mPassCodeManagerInstance = null;

    public static PassCodeManager getPassCodeManager() {
        if (mPassCodeManagerInstance == null) {
            mPassCodeManagerInstance = new PassCodeManager();
        }
        return mPassCodeManagerInstance;
    }

    private Long mTimestamp = 0l;
    private int mVisibleActivitiesCounter = 0;

    protected PassCodeManager() {};

    public void onActivityCreated(Activity activity) {
        if (passCodeIsEnabled()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public void onActivityStarted(Activity activity) {
        if (!sExemptOfPasscodeActivites.contains(activity.getClass()) &&
                passCodeShouldBeRequested()
                ){

            Intent i = new Intent(MainApp.getAppContext(), PassCodeActivity.class);
            i.setAction(PassCodeActivity.ACTION_REQUEST);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(i);

        }

        mVisibleActivitiesCounter++;    // keep it AFTER passCodeShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
        PowerManager powerMgr = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (passCodeIsEnabled() && powerMgr != null && !powerMgr.isScreenOn()) {
            activity.moveTaskToBack(true);
        }
    }

    private void setUnlockTimestamp() {
        mTimestamp = System.currentTimeMillis();
    }

    private boolean passCodeShouldBeRequested(){
        if ((System.currentTimeMillis() - mTimestamp) > PASS_CODE_TIMEOUT &&
                mVisibleActivitiesCounter <= 0
                ){
            return passCodeIsEnabled();
        }
        return false;
    }

    private boolean passCodeIsEnabled() {
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
        return (appPrefs.getBoolean("set_pincode", false));
    }

}
