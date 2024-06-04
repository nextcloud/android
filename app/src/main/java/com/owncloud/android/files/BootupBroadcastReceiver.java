/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.files;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.network.WalledCheckCache;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * App-registered receiver catching the broadcast intent reporting that the system was
 * just boot up.
 */
public class BootupBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = BootupBroadcastReceiver.class.getSimpleName();

    @Inject AppPreferences preferences;
    @Inject UserAccountManager accountManager;
    @Inject UploadsStorageManager uploadsStorageManager;
    @Inject ConnectivityService connectivityService;
    @Inject PowerManagementService powerManagementService;
    @Inject BackgroundJobManager backgroundJobManager;
    @Inject Clock clock;
    @Inject ViewThemeUtils viewThemeUtils;
    @Inject WalledCheckCache walledCheckCache;
    @Inject SyncedFolderProvider syncedFolderProvider;

    /**
     * Receives broadcast intent reporting that the system was just boot up. *
     *
     * @param context The context where the receiver is running.
     * @param intent  The intent received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        AndroidInjection.inject(this, context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MainApp.initSyncOperations(preferences,
                                       uploadsStorageManager,
                                       accountManager,
                                       connectivityService,
                                       powerManagementService,
                                       backgroundJobManager,
                                       clock,
                                       viewThemeUtils,
                                       walledCheckCache,
                                       syncedFolderProvider
                                       );
            MainApp.initContactsBackup(accountManager, backgroundJobManager);
        } else {
            Log_OC.d(TAG, "Getting wrong intent: " + intent.getAction());
        }
    }
}
