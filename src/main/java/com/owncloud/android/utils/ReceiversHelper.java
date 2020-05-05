/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.UploadsStorageManager;

/**
 * Helper for setting up network and power receivers
 */
public final class ReceiversHelper {

    private ReceiversHelper() {
        // utility class -> private constructor
    }

    public static void registerNetworkChangeReceiver(final UploadsStorageManager uploadsStorageManager,
                                                     final UserAccountManager accountManager,
                                                     final ConnectivityService connectivityService,
                                                     final PowerManagementService powerManagementService) {
        Context context = MainApp.getAppContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (connectivityService.getConnectivity().isConnected()) {
                    FilesSyncHelper.restartJobsIfNeeded(uploadsStorageManager,
                                                        accountManager,
                                                        connectivityService,
                                                        powerManagementService);
                }
            }
        };

        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void registerPowerChangeReceiver(
        final UploadsStorageManager uploadsStorageManager,
        final UserAccountManager accountManager,
        final ConnectivityService connectivityService,
        final PowerManagementService powerManagementService
    ) {
        Context context = MainApp.getAppContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
                    FilesSyncHelper.restartJobsIfNeeded(uploadsStorageManager,
                                                        accountManager,
                                                        connectivityService,
                                                        powerManagementService);
                }
            }
        };

        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void registerPowerSaveReceiver(
        final UploadsStorageManager uploadsStorageManager,
        final UserAccountManager accountManager,
        final ConnectivityService connectivityService,
        final PowerManagementService powerManagementService
        ) {
        Context context = MainApp.getAppContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!powerManagementService.isPowerSavingEnabled()) {
                    FilesSyncHelper.restartJobsIfNeeded(uploadsStorageManager,
                                                        accountManager,
                                                        connectivityService,
                                                        powerManagementService);
                }
            }
        };

        context.registerReceiver(broadcastReceiver, intentFilter);
    }
}
