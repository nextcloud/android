/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2016 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartek Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.ui.events.DialogEvent
import com.owncloud.android.ui.events.DialogEventType
import com.owncloud.android.utils.FileStorageUtils
import org.greenrobot.eventbus.EventBus

@Suppress("LongParameterList")
class SynchronizeFileOperation : SyncOperation {

    var localFile: OCFile? = null
        private set

    private var remotePath: String
    private var serverFile: OCFile? = null
    private val user: User
    private val syncFileContents: Boolean
    private val context: Context?
    private val useWorkerWithNotification: Boolean
    private val postDialogEvent: Boolean
    private val allowUploads: Boolean = true

    var transferWasRequested = false
        private set

    constructor(
        remotePath: String,
        user: User,
        syncFileContents: Boolean,
        context: Context?,
        storageManager: FileDataStorageManager,
        useWorkerWithNotification: Boolean,
        postDialogEvent: Boolean
    ) : super(storageManager) {
        this.remotePath = remotePath
        this.user = user
        this.syncFileContents = syncFileContents
        this.context = context
        this.useWorkerWithNotification = useWorkerWithNotification
        this.postDialogEvent = postDialogEvent
    }

    constructor(
        localFile: OCFile?,
        serverFile: OCFile?,
        user: User,
        syncFileContents: Boolean,
        context: Context?,
        storageManager: FileDataStorageManager,
        useWorkerWithNotification: Boolean
    ) : super(storageManager) {
        require(localFile != null || serverFile != null) { "Both serverFile and localFile are NULL" }

        this.localFile = localFile
        this.serverFile = serverFile
        this.remotePath = when {
            localFile != null -> {
                require(serverFile == null || serverFile.remotePath == localFile.remotePath) {
                    "serverFile and localFile do not correspond to the same OC file"
                }
                localFile.remotePath
            }

            else -> serverFile!!.remotePath
        }
        this.user = user
        this.syncFileContents = syncFileContents
        this.context = context
        this.useWorkerWithNotification = useWorkerWithNotification
        this.postDialogEvent = false
    }

    override fun run(client: OwnCloudClient?): RemoteOperationResult<*> {
        transferWasRequested = false

        localFile = localFile ?: storageManager.getFileByPath(remotePath)

        val result = if (localFile?.isDown == false) {
            requestForDownload(localFile)
            RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
        } else {
            syncWithServer(client)
        }

        Log_OC.i(TAG, "Synchronizing ${user.accountName}, file ${localFile?.remotePath}: ${result.logMessage}")

        if (postDialogEvent) {
            EventBus.getDefault().post(DialogEvent(DialogEventType.SYNC))
        }

        return result
    }

    private fun syncWithServer(client: OwnCloudClient?): RemoteOperationResult<*> {
        serverFile = serverFile ?: fetchServerFile(client).let { (result, file) ->
            if (file == null) return result!!
            file
        }

        return serverFile
            ?.let { resolveChanges(it) }
            ?: handleMissingRemoteFile()
    }

    private fun fetchServerFile(client: OwnCloudClient?): Pair<RemoteOperationResult<*>?, OCFile?> {
        val result = ReadFileRemoteOperation(remotePath).execute(client)
        return when {
            result?.isSuccess == true -> {
                val file = FileStorageUtils.fillOCFile(result.data[0] as RemoteFile?).apply {
                    lastSyncDateForProperties = System.currentTimeMillis()
                }
                null to file
            }

            result?.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> null to null

            else -> result to null
        }
    }

    private fun resolveChanges(serverFile: OCFile): RemoteOperationResult<*> {
        val serverChanged = isServerChanged(serverFile)
        val localChanged = localFile?.let {
            it.localModificationTimestamp > it.lastSyncDateForData
        } ?: false

        val result = when {
            localChanged && serverChanged -> handleConflict(serverFile)
            localChanged -> handleLocalChange()
            serverChanged -> handleServerChange(serverFile)
            else -> RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
        }

        if (result.code != RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            storageManager.saveConflict(localFile, null)
        }

        return result
    }

    private fun isServerChanged(serverFile: OCFile): Boolean = if (localFile?.etag.isNullOrEmpty()) {
        serverFile.modificationTimestamp != localFile?.modificationTimestampAtLastSyncForData
    } else {
        serverFile.etag != localFile?.etag
    }

    private fun handleConflict(serverFile: OCFile): RemoteOperationResult<*> {
        storageManager.saveConflict(localFile, serverFile.etag)
        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.SYNC_CONFLICT)
    }

    private fun handleLocalChange(): RemoteOperationResult<*> {
        if (syncFileContents && allowUploads) {
            requestForUpload(localFile)
        } else {
            Log_OC.d(TAG, "Nothing to do here")
        }
        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
    }

    private fun handleServerChange(serverFile: OCFile): RemoteOperationResult<*> {
        localFile?.remoteId = serverFile.remoteId

        if (syncFileContents) {
            requestForDownload(localFile)
        } else {
            val local =
                localFile ?: return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
            val updatedServerFile = serverFile.apply {
                isFavorite = local.isFavorite
                isHidden = local.shouldHide()
                lastSyncDateForData = local.lastSyncDateForData
                setStoragePath(local.storagePath)
                parentId = local.parentId
                etag = local.etag
            }
            storageManager.saveFile(updatedServerFile)
        }
        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
    }

    private fun handleMissingRemoteFile(): RemoteOperationResult<*> {
        val deleted = storageManager.removeFile(localFile, true, true)
        if (!deleted) {
            Log_OC.e(TAG, "Removal of local copy failed (remote file does not exist any longer).")
        }
        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.FILE_NOT_FOUND)
    }

    private fun requestForUpload(file: OCFile?) {
        FileUploadHelper.instance().uploadUpdatedFile(
            user,
            arrayOf(file),
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
            NameCollisionPolicy.OVERWRITE
        )
        transferWasRequested = true
    }

    private fun requestForDownload(file: OCFile?) {
        val file = file ?: return
        val fileDownloadHelper = FileDownloadHelper.instance()

        if (useWorkerWithNotification) {
            Log_OC.d(TAG, "downloading file with worker: ${file.fileName}")
            fileDownloadHelper.downloadFile(user, file)
            transferWasRequested = true
        } else {
            Log_OC.d(TAG, "downloading file without worker: ${file.fileName}")
            runCatching {
                val operation = DownloadFileOperation(user, file, context)
                val result = operation.execute(client)
                transferWasRequested = true
                if (result.isSuccess) {
                    fileDownloadHelper.saveFile(file, operation, storageManager)
                    Log_OC.d(TAG, "requestForDownload completed for: ${file.fileName}")
                } else {
                    Log_OC.d(TAG, "requestForDownload failed for: ${file.fileName}")
                }
            }.onFailure { e ->
                Log_OC.d(TAG, "Exception caught at requestForDownload: $e")
            }
        }
    }

    companion object {
        private val TAG: String = SynchronizeFileOperation::class.java.simpleName
    }
}
