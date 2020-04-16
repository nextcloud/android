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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

public class DeckActionOverrideImpl implements DeckActionOverride {

    private static final String TAG = DeckActionOverrideImpl.class.getSimpleName();

    private static final String APP_NAME = "deck";
    private static final String DECK_APP_ID_BASE = "it.niedermann.nextcloud.deck";
    private static final String[] DECK_APP_ID_FLAVOR_SUFFIXES = new String[]{"", ".play", ".dev"};
    private static final String DECK_ACTIVITY_TO_START = "it.niedermann.nextcloud.deck.ui.PushNotificationActivity";

    private final Context context;

    public DeckActionOverrideImpl(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public Optional<PendingIntent> handleNotification(@NonNull Notification notification, @NonNull User user) {
        if (!APP_NAME.equalsIgnoreCase(notification.app)) {
            return Optional.empty();
        }
        final Intent intent = new Intent();
        for (String flavor : DECK_APP_ID_FLAVOR_SUFFIXES) {
            intent.setClassName(DECK_APP_ID_BASE + flavor, DECK_ACTIVITY_TO_START);
            if (context.getPackageManager().resolveActivity(intent, 0) != null) {
                Log.i(TAG, "Found deck app flavor \"" + flavor + "\"");
                return Optional.of(PendingIntent.getActivity(context, 0, intent
                    .putExtra("account", user.getAccountName())
                    .putExtra("link", notification.getLink())
                    .putExtra("objectId", notification.getObjectId())
                    .putExtra("subject", notification.getSubject())
                    .putExtra("subjectRich", notification.getSubjectRich())
                    .putExtra("message", notification.getMessage())
                    .putExtra("messageRich", notification.getMessageRich())
                    .putExtra("user", notification.getUser())
                    .putExtra("nid", notification.getNotificationId())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_ONE_SHOT));
            }
        }
        return Optional.empty();
    }
}
