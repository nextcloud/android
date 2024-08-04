/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.common.NextcloudClient;
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
    private NextcloudClient client;
    private NotificationsContract.View notificationsActivity;

    public DeleteNotificationTask(NextcloudClient client, Notification notification,
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
