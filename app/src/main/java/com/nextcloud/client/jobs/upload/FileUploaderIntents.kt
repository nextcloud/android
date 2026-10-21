/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen

class FileUploaderIntents(private val context: Context) {

    fun openUploadListIntent(operation: UploadFileOperation?): PendingIntent {
        val intent = NavigatorActivity.intent(context, NavigatorScreen.UploadList).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(FileActivity.EXTRA_USER, operation?.user)
        }

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}
