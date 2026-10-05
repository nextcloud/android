/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.synchronize

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class TwoWaySyncOperation(
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val cancellationRequested: AtomicBoolean
) {

    companion object {
        private val TAG = TwoWaySyncOperation::class.java.simpleName
    }

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var powerManagementService: PowerManagementService

    init {
        MainApp.getAppComponent().inject(this)
    }

    fun ensureLocalFolderExists(folder: OCFile) {
        val storagePath = folder.storagePath?.takeIf { it.isNotEmpty() }
            ?: FileStorageUtils.getDefaultSavePathFor(user.accountName, folder).also {
                folder.storagePath = it
                storageManager.saveFile(folder)
            }

        val directory = File(storagePath)
        if (!directory.exists() && !directory.mkdirs()) {
            Log_OC.e(TAG, "Could not create local directory for internal two-way sync folder: $storagePath")
        }
    }

    /**
     * A missing parent directory means the storage itself is unavailable (e.g. unmounted SD card),
     * which must not be mistaken for the user deleting the file.
     */
    fun isDeletedLocally(file: OCFile?): Boolean {
        val storagePath = file
            ?.takeUnless { it.isFolder }
            ?.storagePath
            ?.takeIf { it.isNotEmpty() }
            ?: return false

        val localFile = File(storagePath)
        return !localFile.exists() && localFile.parentFile?.exists() == true
    }

    fun deleteRemoteFile(file: OCFile, client: OwnCloudClient): Boolean {
        val result = RemoveFileOperation(
            file = file,
            onlyLocalCopy = false,
            user = user,
            isInBackground = false,
            context = context,
            storageManager = storageManager
        ).execute(client)

        if (result.isSuccess) {
            Log_OC.d(TAG, "Deleted remote file after local removal: ${file.fileName}")
        } else {
            Log_OC.w(TAG, "Failed to delete remote file after local removal: ${file.fileName}")
        }

        return result.isSuccess
    }

    fun uploadNewLocalEntries(folder: OCFile, client: OwnCloudClient) {
        val localEntries = folder.storagePath?.let { File(it).listFiles() } ?: return
        val knownNames = storageManager.getFolderContent(folder, false).mapTo(HashSet()) { it.fileName }

        localEntries
            .filterNot { it.name in knownNames }
            .sortedByDescending { it.isDirectory }
            .asSequence()
            .takeWhile { !cancellationRequested.get() }
            .forEach { entry ->
                when {
                    entry.isDirectory -> createRemoteFolder(folder, entry, client)
                    entry.isFile -> uploadNewFile(folder, entry, client)
                }
            }
    }

    private fun createRemoteFolder(parent: OCFile, directory: File, client: OwnCloudClient) {
        val remotePath = parent.remotePath + directory.name + OCFile.PATH_SEPARATOR
        val result = CreateFolderOperation(remotePath, user, context, storageManager).execute(client)

        if (!result.isSuccess) {
            Log_OC.w(TAG, "Failed to create remote folder for: ${directory.name}")
            return
        }

        Log_OC.d(TAG, "Created remote folder for: ${directory.name}")
        SynchronizeFolderOperation(context, remotePath, user, storageManager, false, true).execute(context)
    }

    private fun uploadNewFile(parent: OCFile, file: File, client: OwnCloudClient) {
        val upload = OCUpload(file.absolutePath, parent.remotePath + file.name, user.accountName).apply {
            nameCollisionPolicy = NameCollisionPolicy.DEFAULT
            // the file already lives at its two-way sync location, so it must stay linked as the local copy
            localAction = FileUploadWorker.LOCAL_BEHAVIOUR_COPY
        }

        val result = UploadFileOperation(
            uploadsStorageManager,
            connectivityService,
            powerManagementService,
            user,
            null,
            upload,
            upload.nameCollisionPolicy,
            upload.localAction,
            context,
            false,
            false,
            storageManager
        ).execute(client)

        if (result.isSuccess) {
            Log_OC.d(TAG, "Uploaded new local file: ${file.name}")
        } else {
            Log_OC.w(TAG, "Failed to upload new local file: ${file.name}")
        }
    }
}
