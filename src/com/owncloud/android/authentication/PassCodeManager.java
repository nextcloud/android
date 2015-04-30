package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.PinCodeActivity;

import java.util.HashSet;
import java.util.Set;

public class PassCodeManager {

    private static final Set<Class> sExemptOfPasscodeActivites;

    static {
        sExemptOfPasscodeActivites = new HashSet<Class>();
        sExemptOfPasscodeActivites.add(PinCodeActivity.class);
        // other activities may be exempted, if needed
    }

    private static int PASS_CODE_TIMEOUT = 1000;
        // keeping a "low" value (not 0) is the easiest way to avoid prevent the pass code is requested on rotations

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

    public void onActivityStarted(Activity activity) {
        if (!sExemptOfPasscodeActivites.contains(activity.getClass()) &&
                passCodeShouldBeRequested()
                ){

            Intent i = new Intent(MainApp.getAppContext(), PinCodeActivity.class);
            i.setAction(PinCodeActivity.ACTION_REQUEST);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(i);
        }

        mVisibleActivitiesCounter++;    // AFTER passCodeShouldBeRequested was checked
    }

    public void onActivityStopped(Activity activity) {
        if (mVisibleActivitiesCounter > 0) {
            mVisibleActivitiesCounter--;
        }
        setUnlockTimestamp();
    }

    private boolean passCodeShouldBeRequested(){
        if ((System.currentTimeMillis() - mTimestamp) > PASS_CODE_TIMEOUT &&
                mVisibleActivitiesCounter <= 0
                ){
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
            if (appPrefs.getBoolean("set_pincode", false)) {
                return true;
            }
        }
        return false;
    }

    private void setUnlockTimestamp() {
        mTimestamp = System.currentTimeMillis();
    }

}
