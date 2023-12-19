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

import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.operations.DownloadType
import javax.inject.Inject

class FilesDownloadHelper {

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    init {
        MainApp.getAppComponent().inject(this)
    }

    fun downloadFile(user: User, ocFile: OCFile) {
        backgroundJobManager.startFilesDownloadJob(
            user,
            ocFile,
            "",
            null,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, behaviour: String) {
        backgroundJobManager.startFilesDownloadJob(
            user,
            ocFile,
            behaviour,
            null,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, downloadType: DownloadType) {
        backgroundJobManager.startFilesDownloadJob(
            user,
            ocFile,
            "",
            downloadType,
            "",
            "",
            null
        )
    }

    fun downloadFile(user: User, ocFile: OCFile, conflictUploadId: Long) {
        backgroundJobManager.startFilesDownloadJob(
            user,
            ocFile,
            "",
            null,
            "",
            "",
            conflictUploadId
        )
    }

    fun downloadFile(
        user: User,
        ocFile: OCFile,
        behaviour: String,
        downloadType: DownloadType?,
        activityName: String,
        packageName: String,
        conflictUploadId: Long?
    ) {
        backgroundJobManager.startFilesDownloadJob(
            user,
            ocFile,
            behaviour,
            downloadType,
            activityName,
            packageName,
            conflictUploadId
        )
    }
}
