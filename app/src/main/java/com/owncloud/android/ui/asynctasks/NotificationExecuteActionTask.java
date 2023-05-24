package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.common.OkHttpMethodBase;
import com.nextcloud.operations.DeleteMethod;
import com.nextcloud.operations.GetMethod;
import com.nextcloud.operations.PutMethod;
import com.nextcloud.operations.Utf8PostMethod;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.adapter.NotificationListAdapter;

import org.apache.commons.httpclient.HttpStatus;

import java.io.IOException;

public class NotificationExecuteActionTask extends AsyncTask<Action, Void, Boolean> {

    private NotificationListAdapter.NotificationViewHolder holder;
    private NextcloudClient client;
    private Notification notification;
    private NotificationsActivity notificationsActivity;

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

        if (action.link == null) {
            Log_OC.e(this, "Link is empty!");
            return Boolean.FALSE;
        }

        switch (action.type) {
            case "GET":
                method = new GetMethod(action.link, true);
                break;

            case "POST":
                method = new Utf8PostMethod(action.link, true, null);
                break;

            case "DELETE":
                method = new DeleteMethod(action.link, true);
                break;

            case "PUT":
                method = new PutMethod(action.link, true, null);
                break;

            default:
                // do nothing
                return Boolean.FALSE;
        }

        int status;
        try {
            status = client.execute(method);
        } catch (IOException e) {
            Log_OC.e(this, "Execution of notification action failed: " + e);
            return Boolean.FALSE;
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
