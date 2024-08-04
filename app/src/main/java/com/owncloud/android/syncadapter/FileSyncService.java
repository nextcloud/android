/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-FileCopyrightText: 2011 Sven Aßmann <sven.assmann@lubico.biz>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Background service for synchronizing remote files with their local state.
 * <p>
 * Serves as a connector to an instance of {@link FileSyncAdapter}, as required by standard Android APIs.
 */
public class FileSyncService extends Service {

    // Storage for an instance of the sync adapter
    private static FileSyncAdapter syncAdapter;
    // Object to use as a thread-safe lock
    private static final Object syncAdapterLock = new Object();

    @Inject UserAccountManager userAccountManager;
    @Inject ViewThemeUtils viewThemeUtils;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        synchronized (syncAdapterLock) {
            if (syncAdapter == null) {
                syncAdapter = new FileSyncAdapter(getApplicationContext(), true, userAccountManager, viewThemeUtils);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
