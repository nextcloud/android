/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.autoUpload.handler

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.MediaFoldersDetectionWork
import com.nextcloud.client.jobs.NotificationWork
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.SyncedFolderAdapter

class AutoUploadIntentHandler {

    private var mediaFolderPath: String? = null
    private var mediaFolderType = 0

    fun readMediaFolder(intent: Intent?) {
        if (intent?.extras == null) {
            return
        }

        mediaFolderPath = intent.getStringExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_PATH)
        mediaFolderType = intent.getIntExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_TYPE, -1)
    }

    fun findMediaFolderSection(adapter: SyncedFolderAdapter): Int? {
        val path = mediaFolderPath
        if (path.isNullOrEmpty()) {
            return null
        }

        return adapter.getSectionByLocalPathAndType(path, mediaFolderType).takeIf { it >= 0 }
    }

    fun handleNotification(activity: BaseActivity, userAccountManager: UserAccountManager) {
        val intent = activity.intent?.takeIf { it.extras != null } ?: return

        val accountName = intent.getStringExtra(NotificationWork.KEY_NOTIFICATION_ACCOUNT)
        switchToNotificationAccount(activity, userAccountManager, accountName)

        val notificationId = intent.getIntExtra(MediaFoldersDetectionWork.NOTIFICATION_ID, 0)
        val notificationManager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(notificationId)
    }

    private fun switchToNotificationAccount(
        activity: BaseActivity,
        userAccountManager: UserAccountManager,
        accountName: String?
    ) {
        val currentUser = activity.user.orElse(null)
        if (accountName == null ||
            currentUser == null ||
            accountName.equals(currentUser.accountName, ignoreCase = true)
        ) {
            return
        }

        userAccountManager.setCurrentOwnCloudAccount(accountName)
        activity.setUser(userAccountManager.user)
    }
}
