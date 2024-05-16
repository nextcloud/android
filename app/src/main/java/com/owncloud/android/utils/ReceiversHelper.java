/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.network.WalledCheckCache;
import com.nextcloud.common.DNSCache;
import com.nextcloud.utils.extensions.ContextExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ReceiverFlag;
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
                                                     final PowerManagementService powerManagementService,
                                                     final WalledCheckCache walledCheckCache) {
        Context context = MainApp.getAppContext();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DNSCache.clear();
                walledCheckCache.clear();
                if (connectivityService.getConnectivity().isConnected()) {
                    FilesSyncHelper.restartUploadsIfNeeded(uploadsStorageManager,
                                                           accountManager,
                                                           connectivityService,
                                                           powerManagementService);
                }
            }
        };

        ContextExtensionsKt.registerBroadcastReceiver(context, broadcastReceiver, intentFilter, ReceiverFlag.NotExported);
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
                    FilesSyncHelper.restartUploadsIfNeeded(uploadsStorageManager,
                                                           accountManager,
                                                           connectivityService,
                                                           powerManagementService);
                }
            }
        };

        ContextExtensionsKt.registerBroadcastReceiver(context, broadcastReceiver, intentFilter, ReceiverFlag.NotExported);
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
                    FilesSyncHelper.restartUploadsIfNeeded(uploadsStorageManager,
                                                           accountManager,
                                                           connectivityService,
                                                           powerManagementService);
                }
            }
        };

        ContextExtensionsKt.registerBroadcastReceiver(context, broadcastReceiver, intentFilter, ReceiverFlag.NotExported);
    }
}
