/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Jonas Mayer <jonas.a.mayer@gmx.net>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.notifications

import com.owncloud.android.datamodel.OCFile

object NotificationUtils {
    const val NOTIFICATION_CHANNEL_GENERAL: String = "NOTIFICATION_CHANNEL_GENERAL"
    const val NOTIFICATION_CHANNEL_DOWNLOAD: String = "NOTIFICATION_CHANNEL_DOWNLOAD"
    const val NOTIFICATION_CHANNEL_UPLOAD: String = "NOTIFICATION_CHANNEL_UPLOAD"
    const val NOTIFICATION_CHANNEL_MEDIA: String = "NOTIFICATION_CHANNEL_MEDIA"
    const val NOTIFICATION_CHANNEL_FILE_SYNC: String = "NOTIFICATION_CHANNEL_FILE_SYNC"
    const val NOTIFICATION_CHANNEL_FILE_OBSERVER: String = "NOTIFICATION_CHANNEL_FILE_OBSERVER"
    const val NOTIFICATION_CHANNEL_PUSH: String = "NOTIFICATION_CHANNEL_PUSH"
    const val NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS: String = "NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS"
    const val NOTIFICATION_CHANNEL_OFFLINE_OPERATIONS: String = "NOTIFICATION_CHANNEL_OFFLINE_OPERATIONS"
    const val NOTIFICATION_CHANNEL_CONTENT_OBSERVER: String = "NOTIFICATION_CHANNEL_CONTENT_OBSERVER"

    fun createUploadNotificationTag(file: OCFile): String =
        createUploadNotificationTag(file.remotePath, file.storagePath)

    @JvmStatic
    fun createUploadNotificationTag(remotePath: String?, localPath: String): String = remotePath + localPath
}
