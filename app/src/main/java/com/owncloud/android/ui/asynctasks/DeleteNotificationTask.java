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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.adapter.NotificationListAdapter;
import com.owncloud.android.ui.notifications.NotificationsContract;

public class DeleteNotificationTask extends AsyncTask<Action, Void, Boolean> {
    private Notification notification;
    private NotificationListAdapter.NotificationViewHolder holder;
    private OwnCloudClient client;
    private NotificationsContract.View notificationsActivity;

    public DeleteNotificationTask(OwnCloudClient client, Notification notification,
                                  NotificationListAdapter.NotificationViewHolder holder,
                                  NotificationsActivity notificationsActivity) {
        this.client = client;
        this.notification = notification;
        this.holder = holder;
        this.notificationsActivity = notificationsActivity;
    }

    @Override
    protected void onPreExecute() {
        notificationsActivity.removeNotification(holder);
    }

    @Override
    protected Boolean doInBackground(Action... actions) {
        RemoteOperationResult result = new DeleteNotificationRemoteOperation(notification.notificationId)
            .execute(client);

        return result.isSuccess();
    }

    @Override
    protected void onPostExecute(Boolean success) {
        notificationsActivity.onRemovedNotification(success);
    }
}
