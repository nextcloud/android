/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
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

package com.owncloud.android.files.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Receives all connectivity action from Android OS at all times and performs
 * required OC actions. For now that are: - Signal connectivity to
 * {@link FileUploadService}.
 * 
 * Later can be added: - Signal connectivity to download service, deletion
 * service, ... - Handle offline mode (cf.
 * https://github.com/owncloud/android/issues/162)
 * 
 */
public class ConnectivityActionReceiver extends BroadcastReceiver {
    private static final String TAG = ConnectivityActionReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        // LOG ALL EVENTS:
        Log_OC.v(TAG, "action: " + intent.getAction());
        Log_OC.v(TAG, "component: " + intent.getComponent());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Log_OC.v(TAG, "key [" + key + "]: " + extras.get(key));
            }
        } else {
            Log_OC.v(TAG, "no extras");
        }

        
        /**
         * Just checking for State.CONNECTED will is not good enough, as it ends here multiple times.
         * Work around from:
         * http://stackoverflow.com/
         * questions/17287178/connectivitymanager-getactivenetworkinfo-returning-true-when-internet-is-off
         */            
        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo networkInfo =
                intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected()) {
                Log_OC.d(TAG, "Wifi is connected: " + String.valueOf(networkInfo));
//                wifiConnected(context);
            }
        } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if(networkInfo == null || networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                ! networkInfo.isConnected()) {
                Log_OC.d(TAG, "Wifi is disconnected: " + String.valueOf(networkInfo));
                wifiDisconnected(context);
            }
        }
        
    }

//    private void wifiConnected(Context context) {
//        Log_OC.d(TAG, "FileUploadService.retry() called by onReceive()");
//      FileUploadService.retry(context);
//    }

    private void wifiDisconnected(Context context) {
        
    }

    static public void enableActionReceiver(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName compName = new ComponentName(context.getApplicationContext(), ConnectivityActionReceiver.class);
        pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    static public void disableActionReceiver(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName compName = new ComponentName(context.getApplicationContext(), ConnectivityActionReceiver.class);
        pm.setComponentEnabledSetting(compName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}