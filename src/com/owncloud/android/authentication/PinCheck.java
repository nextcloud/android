package com.owncloud.android.authentication;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.PinCodeActivity;

public class PinCheck extends Activity {

    private static Long timestamp = 0l;
    
    public static void setUnlockTimestamp() {
        timestamp = System.currentTimeMillis();
    }

    public static boolean checkIfPinEntry(){
        if ((System.currentTimeMillis() - timestamp) > 10000){
            SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(MainApp.getAppContext());
            if (appPrefs.getBoolean("set_pincode", false)) {
                return true;
            }
        }
        return false;
    }
}
