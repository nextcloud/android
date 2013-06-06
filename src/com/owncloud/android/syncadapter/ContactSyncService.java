/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

package com.owncloud.android.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ContactSyncService extends Service {
    private static final Object syncAdapterLock = new Object();
    private static AbstractOwnCloudSyncAdapter mSyncAdapter = null;

    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (mSyncAdapter == null) {
                mSyncAdapter = new ContactSyncAdapter(getApplicationContext(),
                        true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

}
