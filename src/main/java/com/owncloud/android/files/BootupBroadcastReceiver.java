/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2012 Bartek Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.files;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * App-registered receiver catching the broadcast intent reporting that the system was 
 * just boot up.
 */
public class BootupBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootupBroadcastReceiver.class.getSimpleName();

    /**
     * Receives broadcast intent reporting that the system was just boot up.
     **
     * @param   context     The context where the receiver is running.
     * @param   intent      The intent received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MainApp.initSyncOperations();
            MainApp.initContactsBackup();
        } else {
            Log_OC.d(TAG, "Getting wrong intent: " + intent.getAction());
        }
    }
}
