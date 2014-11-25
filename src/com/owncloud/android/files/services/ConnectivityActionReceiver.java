package com.owncloud.android.files.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

import com.owncloud.android.files.InstantUploadBroadcastReceiver;

public class ConnectivityActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ConnectivityActionReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            Log.v(TAG, "action: " + intent.getAction());
            Log.v(TAG, "component: " + intent.getComponent());
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Log.v(TAG, "key [" + key + "]: " + extras.get(key));
                }
            } else {
                Log.v(TAG, "no extras");
            }

            if (InstantUploadBroadcastReceiver.isOnline(context)) {
                FileUploadService.retry(context);
            }
        }
    }
    
    static public void enable(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName compName = 
              new ComponentName(context.getApplicationContext(), 
                      ConnectivityActionReceiver.class);
        pm.setComponentEnabledSetting(
              compName,
              PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 
              PackageManager.DONT_KILL_APP);
    }
    
    static public void disable(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName compName = 
              new ComponentName(context.getApplicationContext(), 
                      ConnectivityActionReceiver.class);
        pm.setComponentEnabledSetting(
              compName,
              PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
              PackageManager.DONT_KILL_APP);
    }
}