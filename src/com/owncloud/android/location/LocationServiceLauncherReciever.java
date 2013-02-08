/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
package com.owncloud.android.location;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class LocationServiceLauncherReciever extends BroadcastReceiver {

    private final String TAG = getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent deviceTrackingIntent = new Intent();
        deviceTrackingIntent
                .setAction("com.owncloud.android.location.LocationUpdateService");
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean trackDevice = preferences.getBoolean("enable_devicetracking",
                true);

        // Used in Preferences activity so that tracking is disabled or
        // reenabled
        if (intent.hasExtra("TRACKING_SETTING")) {
            trackDevice = intent.getBooleanExtra("TRACKING_SETTING", true);
        }

        startOrStopDeviceTracking(context, trackDevice);
    }

    /**
     * Used internally. Starts or stops the device tracking service
     * 
     * @param trackDevice true to start the service, false to stop it
     */
    private void startOrStopDeviceTracking(Context context, boolean trackDevice) {
        Intent deviceTrackingIntent = new Intent();
        deviceTrackingIntent
                .setAction("com.owncloud.android.location.LocationUpdateService");
        if (!isDeviceTrackingServiceRunning(context) && trackDevice) {
            Log.d(TAG, "Starting device tracker service");
            context.startService(deviceTrackingIntent);
        } else if (isDeviceTrackingServiceRunning(context) && !trackDevice) {
            Log.d(TAG, "Stopping device tracker service");
            context.stopService(deviceTrackingIntent);
        }
    }

    /**
     * Checks to see whether or not the LocationUpdateService is running
     * 
     * @return true, if it is. Otherwise false
     */
    private boolean isDeviceTrackingServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
