/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import android.content.Context
import android.os.Handler
import android.os.ParcelFileDescriptor
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal class DocumentOpener(private val context: Context, private val scope: CoroutineScope) {

    @Throws(FileNotFoundException::class)
    fun open(document: Document, mode: String?, isSystemServerCaller: Boolean): ParcelFileDescriptor? {
        val ocFile = document.requireFile()
        if (ocFile.isFolder) {
            Log_OC.w(TAG, "openDocument called on a folder, which is not openable: ${document.documentId}")
            return null
        }

        val accessMode = AccessMode.parse(mode)
        prepareLocalFile(document, ocFile, accessMode)
        val openedFile = createOpenedDocumentFile(ocFile, accessMode, isSystemServerCaller)

        if (accessMode.isReadOnly) {
            return openForRead(openedFile)
        }

        return openForWrite(document, ocFile, openedFile, accessMode)
    }

    @Throws(FileNotFoundException::class)
    private fun prepareLocalFile(document: Document, ocFile: OCFile, accessMode: AccessMode) {
        if (accessMode.isTruncate && !ocFile.existsOnDevice()) {
            assignDefaultStoragePath(document.user, ocFile)
        }

        val needsDownload = !accessMode.isTruncate &&
            (!ocFile.existsOnDevice() || (!accessMode.isWriteOnly && hasServerChange(document, ocFile)))
        Log_OC.d(
            TAG,
            "openDocument(), remotePath=${ocFile.remotePath}, storagePath=${ocFile.storagePath}, " +
                "existsOnDevice=${ocFile.existsOnDevice()}, truncate=${accessMode.isTruncate}, " +
                "needsDownload=$needsDownload"
        )

        if (!needsDownload) {
            return
        }

        if (ocFile.localModificationTimestamp > ocFile.lastSyncDateForData) {
            Log_OC.w(TAG, "Conflict found!")
            return
        }

        downloadOffBinderThread(document, ocFile)
    }

    @Throws(FileNotFoundException::class)
    private fun assignDefaultStoragePath(user: User, ocFile: OCFile) {
        val localFile = File(FileStorageUtils.getDefaultSavePathFor(user.accountName, ocFile))
        val parent = localFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw FileNotFoundException("Local folder could not be created: ${parent.absolutePath}")
        }
        ocFile.storagePath = localFile.absolutePath
    }

    @Throws(FileNotFoundException::class)
    private fun hasServerChange(document: Document, ocFile: OCFile): Boolean {
        val result = CheckEtagRemoteOperation(ocFile.remotePath, ocFile.etag).execute(document.user, context)
        return when (result.code) {
            ResultCode.ETAG_CHANGED -> result.data != null

            ResultCode.ETAG_UNCHANGED -> false

            else -> {
                Log_OC.e(TAG, result.toString())
                throw FileNotFoundException("Error synchronizing file: ${ocFile.fileName}")
            }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun downloadOffBinderThread(document: Document, ocFile: OCFile) {
        val errorMessage = "Error downloading file: ${ocFile.fileName}"
        val isDownloaded = try {
            runBlocking(Dispatchers.IO) { downloadFile(document, ocFile) }
        } catch (e: Exception) {
            throw fileNotFoundException(errorMessage, e)
        }

        if (!isDownloaded) {
            throw FileNotFoundException(errorMessage)
        }
    }

    @Throws(FileNotFoundException::class)
    private fun downloadFile(document: Document, ocFile: OCFile): Boolean {
        val operation = DownloadFileOperation(document.user, ocFile, context)
        val result = operation.execute(document.requireClient(context))
        if (result.isSuccess) {
            saveDownloadedFile(document.storageManager, operation, ocFile)
            return true
        }

        if (ocFile.isDown) {
            context.showToast(R.string.file_not_synced)
            return true
        }

        Log_OC.e(TAG, result.toString())
        return false
    }

    private fun saveDownloadedFile(
        storageManager: FileDataStorageManager,
        operation: DownloadFileOperation,
        file: OCFile
    ) {
        val syncDate = System.currentTimeMillis()
        val savePath = operation.savePath
        file.apply {
            lastSyncDateForProperties = syncDate
            lastSyncDateForData = syncDate
            isUpdateThumbnailNeeded = true
            modificationTimestamp = operation.getModificationTimestamp()
            modificationTimestampAtLastSyncForData = operation.getModificationTimestamp()
            etag = operation.etag
            mimeType = operation.mimeType
            storagePath = savePath
            fileLength = File(savePath).length()
            remoteId = operation.file.remoteId
        }
        storageManager.saveFile(file)
        if (MimeTypeUtil.isMedia(operation.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath, file)
        }
        storageManager.saveConflict(file, null)
    }

    @Throws(FileNotFoundException::class)
    private fun createOpenedDocumentFile(
        ocFile: OCFile,
        accessMode: AccessMode,
        isSystemServerCaller: Boolean
    ): OpenedDocumentFile {
        val storagePath = ocFile.storagePath
        if (storagePath.isNullOrEmpty()) {
            throw FileNotFoundException("No local file for ${ocFile.remotePath}")
        }

        val localFile = File(storagePath)
        val descriptorFile = if (isSystemServerCaller) {
            createSystemServerAccessibleCopy(localFile, accessMode.isTruncate)
        } else {
            localFile
        }
        return OpenedDocumentFile(localFile, descriptorFile)
    }

    @Throws(FileNotFoundException::class)
    private fun createSystemServerAccessibleCopy(file: File, truncate: Boolean): File = try {
        File.createTempFile(COPY_PREFIX, null, context.cacheDir).also { copy ->
            if (!truncate && file.exists()) {
                Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    } catch (e: IOException) {
        throw fileNotFoundException("Failed to create copy of ${file.absolutePath}", e)
    }

    @Throws(FileNotFoundException::class)
    private fun openForRead(openedFile: OpenedDocumentFile): ParcelFileDescriptor =
        ParcelFileDescriptor.open(openedFile.descriptorFile, ParcelFileDescriptor.MODE_READ_ONLY)
            .also { openedFile.deleteCopy() }

    @Throws(FileNotFoundException::class)
    private fun openForWrite(
        document: Document,
        ocFile: OCFile,
        openedFile: OpenedDocumentFile,
        accessMode: AccessMode
    ): ParcelFileDescriptor {
        val onClose = ParcelFileDescriptor.OnCloseListener { error ->
            if (error != null) {
                Log_OC.e(TAG, "File was closed with an error: ${ocFile.fileName}", error)
                openedFile.deleteCopy()
                return@OnCloseListener
            }

            scope.launch { uploadWrittenDocument(document, ocFile, openedFile) }
        }

        return try {
            ParcelFileDescriptor.open(openedFile.descriptorFile, accessMode.bits, Handler(context.mainLooper), onClose)
        } catch (e: IOException) {
            throw fileNotFoundException(
                "Failed to open document for writing ${openedFile.descriptorFile.absolutePath}",
                e
            )
        }
    }

    private fun uploadWrittenDocument(document: Document, ocFile: OCFile, openedFile: OpenedDocumentFile) {
        val file = openedFile.localFile
        try {
            openedFile.moveCopyToLocalFile()
        } catch (e: IOException) {
            Log_OC.e(TAG, "Failed to move written copy to ${file.absolutePath}", e)
            openedFile.deleteCopy()
            return
        }

        Log_OC.d(TAG, "openDocument(), closed ${ocFile.remotePath}, length=${file.length()}, starting upload")
        ocFile.fileLength = file.length()
        ocFile.modificationTimestamp = System.currentTimeMillis()
        document.storageManager.saveFile(ocFile)

        FileUploadHelper.instance().uploadUpdatedFile(
            document.user,
            arrayOf<OCFile?>(ocFile),
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
            NameCollisionPolicy.OVERWRITE
        )
    }

    companion object {
        private val TAG = DocumentOpener::class.java.simpleName
        private const val COPY_PREFIX = "document"
    }
}
