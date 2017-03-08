/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.evernote.android.job.JobManager;
import com.owncloud.android.MainApp;

/**
 * Handles shutdown procedure - basically just waits a little bit for all jobs to finish
 */

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (MainApp.getSyncedFolderObserverService() != null) {
            MainApp.getSyncedFolderObserverService().onDestroy();

            // as without GCM pending uploads are uploaded more than once try to cancel them
            JobManager.instance().cancelAll();
        }
    }
}
