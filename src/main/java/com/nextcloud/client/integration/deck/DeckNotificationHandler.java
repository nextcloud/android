package com.nextcloud.client.integration.deck;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nextcloud.client.account.User;
import com.nextcloud.client.integration.NotificationHandler;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

public class DeckNotificationHandler implements NotificationHandler {

    private static final String TAG = DeckNotificationHandler.class.getSimpleName();

    private static final String APP_NAME = "deck";

    private final Context context;

    public DeckNotificationHandler(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Intent handleNotification(@NonNull Notification notification, @NonNull User user) throws AppNotInstalledException, AppCannotHandelNotificationException {
        if (!APP_NAME.equalsIgnoreCase(notification.app)) {
            throw new AppCannotHandelNotificationException();
        }
        final String baseDeckApplicationId = "it.niedermann.nextcloud.deck";
        final String activityToStart = "it.niedermann.nextcloud.deck.ui.PushNotificationActivity";
        final String[] flavors = new String[]{"", ".play", ".dev"};
        for (String flavor : flavors) {
            final Intent intent = new Intent().setClassName(baseDeckApplicationId + flavor, activityToStart);
            if (context.getPackageManager().resolveActivity(
                intent, 0) != null) {
                Log.i(TAG, "Found deck app flavor \"" + flavor + "\"");
                return intent
                    .putExtra("account", user.getAccountName())
                    .putExtra("link", notification.getLink())
                    .putExtra("objectId", notification.getObjectId())
                    .putExtra("subject", notification.getSubject())
                    .putExtra("subjectRich", notification.getSubjectRich())
                    .putExtra("message", notification.getMessage())
                    .putExtra("messageRich", notification.getMessageRich())
                    .putExtra("user", notification.getUser())
                    .putExtra("nid", notification.getNotificationId())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
        }
        Log.v(TAG, "Couldn't find any installed deck app.");
        throw new AppNotInstalledException();
    }
}
