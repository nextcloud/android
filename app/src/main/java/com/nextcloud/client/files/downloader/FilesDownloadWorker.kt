/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.files.downloader

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.utils.theme.ViewThemeUtils

class FilesDownloadWorker(
    private val viewThemeUtils: ViewThemeUtils,
    private val accountManager: UserAccountManager,
    private val uploadsStorageManager: UploadsStorageManager,
    private var localBroadcastManager: LocalBroadcastManager,
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    companion object {
        const val USER = "USER"
        const val FILE = "FILE"
        const val BEHAVIOUR = "BEHAVIOUR"
        const val DOWNLOAD_TYPE = "DOWNLOAD_TYPE"
        const val ACTIVITY_NAME = "ACTIVITY_NAME"
        const val PACKAGE_NAME = "PACKAGE_NAME"
        const val CONFLICT_UPLOAD_ID = "CONFLICT_UPLOAD_ID"
    }

    override fun doWork(): Result {
        return try {
            Result.success()
        } catch (t: Throwable) {
            Result.failure()
        }
    }
}
