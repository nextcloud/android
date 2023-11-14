/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
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

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.notifications.DeleteAllNotificationsRemoteOperation;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.notifications.NotificationsContract;

public class DeleteAllNotificationsTask extends AsyncTask<Action, Void, Boolean> {
    private NextcloudClient client;
    private final NotificationsContract.View notificationsActivity;

    public DeleteAllNotificationsTask(NextcloudClient client, NotificationsActivity notificationsActivity) {
        this.client = client;
        this.notificationsActivity = notificationsActivity;
    }

    @Override
    protected Boolean doInBackground(Action... actions) {

        RemoteOperationResult result = new DeleteAllNotificationsRemoteOperation().execute(client);

        return result.isSuccess();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        notificationsActivity.onRemovedAllNotifications(success);
    }
}
