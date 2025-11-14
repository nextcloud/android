/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.owncloud.android.R
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadNotificationManager(context: Context, viewThemeUtils: ViewThemeUtils, id: Int) :
    WorkerNotificationManager(
        id,
        context,
        viewThemeUtils,
        tickerId = R.string.foreground_service_upload,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD
    )
