/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.files;

import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.observer.FileObserverService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * App-registered receiver catching the broadcast intent reporting that the system was 
 * just boot up.
 */
public class BootupBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = BootupBroadcastReceiver.class.getSimpleName();
    
    /**
     * Receives broadcast intent reporting that the system was just boot up.
     *
     * Starts {@link FileObserverService} to enable observation of favourite files.
     * 
     * @param   context     The context where the receiver is running.
     * @param   intent      The intent received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log_OC.wtf(TAG, "Incorrect action sent " + intent.getAction());
            return;
        }
        Log_OC.d(TAG, "Starting file observer service...");
        Intent initObservers = FileObserverService.makeInitIntent(context);
        context.startService(initObservers);
    }

}
