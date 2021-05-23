/*
 * Nextcloud Android client application
 *
 * @author qidu th
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.owncloud.android.ui.activity.SyncedFoldersActivity;

public class SyncedFoldersTask extends AsyncTask<Void, Void, String> {
    private SyncedFoldersActivity syncedFoldersActivity;
    private int number;
    private boolean force;

    public SyncedFoldersTask(SyncedFoldersActivity activity, int num, boolean f) {
        this.syncedFoldersActivity = activity;
        this.number = num;
        this.force = f;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... voids) {
        syncedFoldersActivity.load(number, force);
        return null;
    }

    @Override
    protected void onPostExecute(String message) {
        super.onPostExecute(message);

        Handler handler = syncedFoldersActivity.getHandler();
        Message msg = handler.obtainMessage(SyncedFoldersActivity.UPDATE_MY_SYNCED_VIEW);
        handler.sendMessage(msg);
    }

}