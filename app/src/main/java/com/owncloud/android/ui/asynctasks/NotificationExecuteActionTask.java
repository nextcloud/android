package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.common.OkHttpMethodBase;
import com.nextcloud.operations.DeleteMethod;
import com.nextcloud.operations.GetMethod;
import com.nextcloud.operations.PostMethod;
import com.nextcloud.operations.PutMethod;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.adapter.NotificationListAdapter;

import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;

public class NotificationExecuteActionTask extends AsyncTask<Action, Void, Boolean> {

    private final NotificationListAdapter.NotificationViewHolder holder;
    private final NextcloudClient client;
    private final Notification notification;
    private final NotificationsActivity notificationsActivity;

    public NotificationExecuteActionTask(NextcloudClient client,
                                         NotificationListAdapter.NotificationViewHolder holder,
                                         Notification notification,
                                         NotificationsActivity notificationsActivity) {
        this.client = client;
        this.holder = holder;
        this.notification = notification;
        this.notificationsActivity = notificationsActivity;
    }

    @Override
    protected Boolean doInBackground(Action... actions) {
        OkHttpMethodBase method;
        Action action = actions[0];

        switch (action.type) {
            case "GET" -> method = new GetMethod(action.link, true);
            case "POST" -> method = new PostMethod(action.link, true, null);
            case "DELETE" -> method = new DeleteMethod(action.link, true);
            case "PUT" -> method = new PutMethod(action.link, true, null);
            default -> {
                // do nothing
                return false;
            }
        }

        method.addRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

        int status;
        try {
            status = client.execute(method);
        } catch (IOException e) {
            Log_OC.e(this, "Execution of notification action failed: " + e);
            return false;
        } finally {
            method.releaseConnection();
        }

        return status == HttpStatus.SC_OK || status == HttpStatus.SC_ACCEPTED;
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        notificationsActivity.onActionCallback(isSuccess, notification, holder);
    }
}
