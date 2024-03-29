/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
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
