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

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.owncloud.android.R;

public class LocationUpdateService extends IntentService implements
        LocationListener {

    public static final String TAG = "LocationUpdateService";

    private LocationManager mLocationManager;
    private LocationProvider mLocationProvider;
    private SharedPreferences mPreferences;

    public LocationUpdateService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        // Determine, how we can track the device
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        mLocationProvider = mLocationManager.getProvider(mLocationManager
                .getBestProvider(criteria, true));

        // Notify user if there is no way to track the device
        if (mLocationProvider == null) {
            String message = String.format(getString(R.string.location_no_provider), getString(R.string.app_name));
            Toast.makeText(this,
                    message,
                    Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        // Get preferences for device tracking
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean trackDevice = mPreferences.getBoolean("enable_devicetracking",
                true);
        int updateIntervall = Integer.parseInt(mPreferences.getString(
                "devicetracking_update_intervall", "30")) * 60 * 1000;
        int distanceBetweenLocationChecks = 50;

        // If we do shall track the device -> Stop
        if (!trackDevice) {
            Log.d(TAG, "Devicetracking is disabled");
            stopSelf();
            return;
        }

        mLocationManager.requestLocationUpdates(mLocationProvider.getName(),
                updateIntervall, distanceBetweenLocationChecks, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: " + location);

    }

    @Override
    public void onProviderDisabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        // TODO Auto-generated method stub

    }

}
