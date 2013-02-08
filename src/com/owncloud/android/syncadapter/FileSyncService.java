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
package com.owncloud.android.syncadapter;

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
    public static final String SYNC_MESSAGE = "ACCOUNT_SYNC";
    public static final String SYNC_FOLDER_REMOTE_PATH = "SYNC_FOLDER_REMOTE_PATH";
    public static final String IN_PROGRESS = "SYNC_IN_PROGRESS";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    public static final String SYNC_RESULT = "SYNC_RESULT";

    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
       return new FileSyncAdapter(getApplicationContext(), true).getSyncAdapterBinder();
    }
}
