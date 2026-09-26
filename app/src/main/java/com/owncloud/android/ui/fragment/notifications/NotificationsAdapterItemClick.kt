/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.notifications

import android.widget.ImageView
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.adapter.NotificationListAdapter

interface NotificationsAdapterItemClick {
    fun onBindIcon(imageView: ImageView, url: String)
    fun deleteNotification(id: Int)
    fun onActionClick(
        holder: NotificationListAdapter.NotificationViewHolder,
        action: Action,
        notification: Notification
    )
}
