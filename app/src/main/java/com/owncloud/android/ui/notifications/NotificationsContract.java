/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.notifications;

import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.adapter.NotificationListAdapter;

public interface NotificationsContract {

    interface View {
        void onRemovedNotification(boolean isSuccess);

        void removeNotification(NotificationListAdapter.NotificationViewHolder holder);

        void onRemovedAllNotifications(boolean isSuccess);

        void onActionCallback(boolean isSuccess,
                              Notification notification,
                              NotificationListAdapter.NotificationViewHolder holder);
    }
}
