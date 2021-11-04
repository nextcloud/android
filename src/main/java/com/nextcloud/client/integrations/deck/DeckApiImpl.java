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

package com.nextcloud.client.integrations.deck;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.nextcloud.client.account.User;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.lib.resources.notifications.models.Notification;

import androidx.annotation.NonNull;

public class DeckApiImpl implements DeckApi {

    static final String APP_NAME = "deck";
    static final String[] DECK_APP_PACKAGES = new String[] {
        "it.niedermann.nextcloud.deck",
        "it.niedermann.nextcloud.deck.play",
        "it.niedermann.nextcloud.deck.dev"
    };
    static final String DECK_ACTIVITY_TO_START = "it.niedermann.nextcloud.deck.ui.PushNotificationActivity";

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_LINK = "link";
    private static final String EXTRA_OBJECT_ID = "objectId";
    private static final String EXTRA_SUBJECT = "subject";
    private static final String EXTRA_SUBJECT_RICH = "subjectRich";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_MESSAGE_RICH = "messageRich";
    private static final String EXTRA_USER = "user";
    private static final String EXTRA_NID = "nid";

    private final Context context;
    private final PackageManager packageManager;

    public DeckApiImpl(@NonNull Context context, @NonNull PackageManager packageManager) {
        this.context = context;
        this.packageManager = packageManager;
    }

    @NonNull
    @Override
    public Optional<PendingIntent> createForwardToDeckActionIntent(@NonNull Notification notification, @NonNull User user) {
        if (APP_NAME.equalsIgnoreCase(notification.app)) {
            final Intent intent = new Intent();
            for (String appPackage : DECK_APP_PACKAGES) {
                intent.setClassName(appPackage, DECK_ACTIVITY_TO_START);
                if (packageManager.resolveActivity(intent, 0) != null) {
                    return Optional.of(createPendingIntent(intent, notification, user));
                }
            }
        }
        return Optional.empty();
    }

    private PendingIntent createPendingIntent(@NonNull Intent intent, @NonNull Notification notification, @NonNull User user) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, notification.getNotificationId(),
                                         putExtrasToIntent(intent, notification, user),
                                         PendingIntent.FLAG_ONE_SHOT);
    }

    private Intent putExtrasToIntent(@NonNull Intent intent, @NonNull Notification notification, @NonNull User user) {
        return intent
            .putExtra(EXTRA_ACCOUNT, user.getAccountName())
            .putExtra(EXTRA_LINK, notification.getLink())
            .putExtra(EXTRA_OBJECT_ID, notification.getObjectId())
            .putExtra(EXTRA_SUBJECT, notification.getSubject())
            .putExtra(EXTRA_SUBJECT_RICH, notification.getSubjectRich())
            .putExtra(EXTRA_MESSAGE, notification.getMessage())
            .putExtra(EXTRA_MESSAGE_RICH, notification.getMessageRich())
            .putExtra(EXTRA_USER, notification.getUser())
            .putExtra(EXTRA_NID, notification.getNotificationId());
    }
}
