/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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
package eu.alefzero.owncloud.syncadapter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Background service for syncing files to our local Database
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileSyncService extends Service {
    public static final String SYNC_MESSAGE = "eu.alefzero.owncloud.files.ACCOUNT_SYNC";
    public static final String IN_PROGRESS = "sync_in_progress";
    public static final String ACCOUNT_NAME = "account_name";

    private static final Object syncAdapterLock = new Object();
    private static AbstractOwnCloudSyncAdapter concretSyncAdapter = null;

    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        synchronized (syncAdapterLock) {
            if (concretSyncAdapter == null)
                concretSyncAdapter = new FileSyncAdapter(
                        getApplicationContext(), true);
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return concretSyncAdapter.getSyncAdapterBinder();
    }
}
