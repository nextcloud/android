/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.filedisplayactivity

import android.content.Context
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.model.OCUploadLocalPathData
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.fragment.filesRepository.FilesRepository
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileDisplayActivityRepositoryImpl(
    private val context: Context,
    private val clientRepository: ClientRepository,
    private val filesRepository: FilesRepository,
    private val storageManager: FileDataStorageManager,
    private val backgroundJobManager: BackgroundJobManager
) : FileDisplayActivityRepository {

    private val user
        get() = storageManager.user

    private val capabilities
        get() = CapabilityUtils.getCapability(user, context)

    override suspend fun fetchRecommendedFiles(ignoreETag: Boolean, folder: OCFile?): ArrayList<OCFile>? {
        if (folder?.isRootDirectory == false || capabilities.recommendations.isFalse) {
            return null
        }

        return filesRepository.fetchRecommendedFiles(user.accountName, ignoreETag, storageManager)
    }

    override suspend fun syncFolder(folder: OCFile, ignoreETag: Boolean): RemoteOperationResult<*> {
        val client = clientRepository.getOwncloudClient()
            ?: return RemoteOperationResult<RemoteOperationResult.ResultCode>(
                RemoteOperationResult.ResultCode.UNKNOWN_ERROR
            )

        return withContext(Dispatchers.IO) {
            RefreshFolderOperation(
                folder,
                System.currentTimeMillis(),
                false,
                ignoreETag,
                storageManager,
                user,
                context
            ).execute(client)
        }
    }

    override fun downloadFileIfNotStartedBefore(file: OCFile) {
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, file)
    }

    override fun downloadFile(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String) {
        val downloadHelper = FileDownloadHelper.instance()
        if (downloadHelper.isDownloading(user, file)) {
            return
        }

        downloadHelper.downloadFile(
            user,
            file,
            downloadBehaviour,
            DownloadType.DOWNLOAD,
            activityName,
            packageName,
            null
        )
    }

    override fun uploadFiles(localPaths: Array<String>, remotePaths: Array<String>, localBehaviour: Int) {
        val data = OCUploadLocalPathData.forFile(
            user,
            localPaths,
            remotePaths,
            localBehaviour,
            createRemoteFolder = true
        )
        FileUploadHelper.instance().uploadNewFiles(data)
    }

    override fun startMetadataSync(remotePath: String, folderAlreadySynced: Boolean) {
        backgroundJobManager.startMetadataSyncJob(remotePath, folderAlreadySynced)
    }
}
