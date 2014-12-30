package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.owncloud.android.MainApp;

public class PinCheck extends Activity {

    private static Long timestamp = 0l;
    private static Long lastStart = 0l;
    
    public static void setUnlockTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    public static boolean checkIfPinEntry(){
        if ((System.currentTimeMillis() - timestamp) > 10000 &&
            (System.currentTimeMillis() - lastStart) > 10000){
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
            if (appPrefs.getBoolean("set_pincode", false)) {
                lastStart = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }
}
