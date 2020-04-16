package com.nextcloud.client.integration;

import android.content.Intent;

import com.nextcloud.client.account.User;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

public interface NotificationHandler {

    @NonNull
    Intent handleNotification(@NonNull final Notification notification,
                              @NonNull final User user) throws AppNotInstalledException, AppCannotHandelNotificationException;

    class AppNotInstalledException extends Exception {
    }

    class AppCannotHandelNotificationException extends Exception {
    }
}
