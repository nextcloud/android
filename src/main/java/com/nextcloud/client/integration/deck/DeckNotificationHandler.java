/*
 * Nextcloud application
 *
 * @author Stefan Niedermann
 * Copyright (C) 2020 Stefan Niedermann <info@niedermann.it>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.integration.deck;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nextcloud.client.account.User;
import com.nextcloud.client.integration.AppCannotHandelNotificationException;
import com.nextcloud.client.integration.AppNotInstalledException;
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
