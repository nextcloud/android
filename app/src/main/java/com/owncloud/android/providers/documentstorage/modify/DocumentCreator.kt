/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.modify

import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.providers.documentstorage.model.DocumentResolver
import com.owncloud.android.providers.documentstorage.sync.DocumentNotifier
import com.owncloud.android.providers.documentstorage.sync.FolderRefresher
import com.owncloud.android.providers.documentstorage.util.fileNotFoundException
import com.owncloud.android.providers.documentstorage.util.requireSuccess
import android.content.Context
import android.provider.DocumentsContract
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.utils.AppConstants
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class DocumentCreator(
    private val context: Context,
    private val accountManager: UserAccountManager,
    private val resolver: DocumentResolver,
    private val nameValidator: DocumentNameValidator,
    private val folderRefresher: FolderRefresher,
    private val notifier: DocumentNotifier
) {

    @Throws(FileNotFoundException::class)
    fun create(documentId: String, mimeType: String?, displayName: String): String? {
        val errorMessage = nameValidator.fileNameError(displayName)
        if (errorMessage != null) {
            context.showToast(errorMessage)
            return null
        }

        val folderDocument = resolver.toDocument(documentId)
        if (!folderDocument.requireFile().canCreateFileAndFolder()) {
            context.showToast(R.string.document_storage_provider_cannot_create_file_and_folder)
            return null
        }

        return if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType, ignoreCase = true)) {
            createFolder(folderDocument, displayName)
        } else {
            createFile(folderDocument, displayName, mimeType)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun createFolder(targetFolder: Document, displayName: String): String? {
        val targetFile = targetFolder.requireFile()
        if (!targetFile.canCreateFileAndFolder()) {
            context.showToast(R.string.document_storage_provider_cannot_create_folder_inside_folder)
            return null
        }

        val newDirPath = targetFile.remotePath + displayName + OCFile.PATH_SEPARATOR
        val storageManager = targetFolder.storageManager
        val client = targetFolder.requireClient(context)

        CreateFolderOperation(newDirPath, accountManager.user, context, storageManager)
            .execute(client)
            .requireSuccess(TAG) {
                "Failed to create document with name $displayName and documentId ${targetFolder.documentId}"
            }

        folderRefresher.refresh(targetFolder, storageManager, targetFolder.user, client)
            .requireSuccess(TAG) { "Failed to create document with documentId ${targetFolder.documentId}" }

        val newFolderId = Document.fromPath(storageManager, newDirPath).documentId
        notifier.notifyChanged(targetFolder)
        return newFolderId
    }

    @Throws(FileNotFoundException::class)
    private fun createFile(targetFolder: Document, displayName: String, mimeType: String?): String? {
        val targetFile = targetFolder.requireFile()
        if (!targetFile.canCreateFileAndFolder()) {
            context.showToast(R.string.document_storage_provider_cannot_create_file_inside_folder)
            return null
        }

        val user = targetFolder.user
        val emptyFile = createEmptyTempFile(user, displayName)
        val newFilePath = targetFile.remotePath + displayName

        try {
            val client = targetFolder.requireClient(context)
            uploadEmptyFile(client, emptyFile, newFilePath, mimeType)

            val updateParent = folderRefresher.refresh(targetFolder, targetFolder.storageManager, user, client)
            showToastIfMaintenanceMode(updateParent)
            updateParent.requireSuccess(TAG) { "Failed to create document with documentId ${targetFolder.documentId}" }

            val newFileId = Document.fromPath(targetFolder.storageManager, newFilePath).documentId
            notifier.notifyChanged(targetFolder)
            return newFileId
        } finally {
            if (emptyFile.exists() && !emptyFile.delete()) {
                Log_OC.w(TAG, "Temp file could not be deleted: ${emptyFile.absolutePath}")
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun createEmptyTempFile(user: User, displayName: String): File {
        val tempDir = File(FileStorageUtils.getTemporalPath(user.accountName))
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw FileNotFoundException("Temp folder could not be created: ${tempDir.absolutePath}")
        }

        val emptyFile = File(tempDir, displayName)
        if (emptyFile.exists() && !emptyFile.delete()) {
            throw FileNotFoundException("Previous file could not be deleted")
        }

        createNewEmptyFile(emptyFile)
        return emptyFile
    }

    @Throws(FileNotFoundException::class)
    private fun createNewEmptyFile(file: File) {
        val isCreated = try {
            file.createNewFile()
        } catch (e: IOException) {
            throw fileNotFoundException(FILE_CREATION_ERROR, e)
        }

        if (!isCreated) {
            throw FileNotFoundException(FILE_CREATION_ERROR)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun uploadEmptyFile(client: OwnCloudClient, emptyFile: File, newFilePath: String, mimeType: String?) {
        val nowSeconds = System.currentTimeMillis() / AppConstants.MILLIS_PER_SECOND
        val result = UploadFileRemoteOperation(
            emptyFile.absolutePath,
            newFilePath,
            mimeType,
            "",
            nowSeconds,
            FileUtil.getCreationTimestamp(emptyFile),
            false
        ).execute(client)

        showToastIfMaintenanceMode(result)
        result.requireSuccess(TAG) { "Failed to upload document with path $newFilePath" }
    }

    private fun showToastIfMaintenanceMode(result: RemoteOperationResult<*>) {
        if (result.code == RemoteOperationResult.ResultCode.MAINTENANCE_MODE) {
            context.showToast(R.string.maintenance_mode)
        }
    }

    companion object {
        private val TAG = DocumentCreator::class.java.simpleName
        private const val FILE_CREATION_ERROR = "File could not be created"
    }
}
