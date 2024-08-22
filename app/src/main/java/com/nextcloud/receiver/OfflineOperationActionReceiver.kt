/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.datamodel.FileDataStorageManager

class OfflineOperationActionReceiver : BroadcastReceiver() {
    companion object {
        const val FILE_PATH = "FILE_PATH"
        const val USER = "USER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val path = intent?.getStringExtra(FILE_PATH) ?: return
        val user = intent.getParcelableArgument(USER, User::class.java) ?: return
        val fileDataStorageManager = FileDataStorageManager(user, context?.contentResolver)
        fileDataStorageManager.offlineOperationDao.deleteByPath(path)
        // TODO Update notification
    }
}
