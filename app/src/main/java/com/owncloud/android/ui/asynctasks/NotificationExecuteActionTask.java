package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.adapter.NotificationListAdapter;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.Utf8PostMethod;

import java.io.IOException;

public class NotificationExecuteActionTask extends AsyncTask<Action, Void, Boolean> {

    private NotificationListAdapter.NotificationViewHolder holder;
    private OwnCloudClient client;
    private Notification notification;
    private NotificationsActivity notificationsActivity;

    public NotificationExecuteActionTask(OwnCloudClient client,
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
        HttpMethod method;
        Action action = actions[0];

        switch (action.type) {
            case "GET":
                method = new GetMethod(action.link);
                break;

            case "POST":
                method = new Utf8PostMethod(action.link);
                break;

            case "DELETE":
                method = new DeleteMethod(action.link);
                break;

            case "PUT":
                method = new PutMethod(action.link);
                break;

            default:
                // do nothing
                return Boolean.FALSE;
        }

        method.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

        int status;
        try {
            status = client.executeMethod(method);
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
