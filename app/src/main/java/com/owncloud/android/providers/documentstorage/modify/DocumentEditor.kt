/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.modify

import android.content.Context
import android.os.SystemClock
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.providers.documentstorage.model.DocumentResolver
import com.owncloud.android.providers.documentstorage.open.PendingDocumentWrite
import com.owncloud.android.providers.documentstorage.open.PendingDocumentWrites
import com.owncloud.android.providers.documentstorage.open.PendingWriteState
import com.owncloud.android.providers.documentstorage.sync.DocumentNotifier
import com.owncloud.android.providers.documentstorage.sync.FolderRefresher
import com.owncloud.android.providers.documentstorage.util.requireSuccess
import dagger.Lazy
import java.io.FileNotFoundException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DocumentEditor(
    private val context: Context,
    private val resolver: DocumentResolver,
    private val notifier: DocumentNotifier,
    private val folderRefresher: FolderRefresher,
    private val nameValidator: DocumentNameValidator,
    private val fileUploadHelper: Lazy<FileUploadHelper>,
    private val pendingWrites: PendingDocumentWrites,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val sleep: (Long) -> Unit = Thread::sleep
) {

    @Volatile
    var isFolderPathValid = true
        private set

    @Throws(FileNotFoundException::class)
    fun rename(documentId: String, displayName: String) {
        val errorMessage = nameValidator.fileNameError(displayName)
        if (errorMessage != null) {
            context.showToast(errorMessage)
            return
        }

        val document = resolver.toDocument(documentId)
        val file = document.requireFile()
        if (!file.canRename()) {
            context.showToast(R.string.document_storage_provider_cannot_rename)
            return
        }

        awaitPendingUploads(document, file)
        val result = RenameFileOperation(file.remotePath, displayName, document.storageManager)
            .execute(document.requireClient(context))
        result.requireSuccess(TAG) { "Failed to rename document with documentId $documentId: ${result.exception}" }

        document.parent?.let(notifier::notifyChanged)
    }

    @Throws(FileNotFoundException::class)
    fun copy(sourceDocumentId: String, targetParentDocumentId: String): String? {
        val targetFolder = resolver.toDocument(targetParentDocumentId)
        val targetFile = targetFolder.requireFile()
        if (!isValidTargetFolder(targetFile)) {
            return null
        }

        val document = resolver.toDocument(sourceDocumentId)
        val sourceFile = document.requireFile()
        val storageManager = document.storageManager
        val failureMessage = "Failed to copy document with documentId $sourceDocumentId to $targetParentDocumentId"

        awaitPendingUploads(document, sourceFile)
        CopyFileOperation(sourceFile.remotePath, targetFile.remotePath, storageManager)
            .execute(document.requireClient(context))
            .requireSuccess(TAG) { failureMessage }

        folderRefresher
            .refresh(targetFolder, storageManager, document.user, targetFolder.requireClient(context))
            .requireSuccess(TAG) { failureMessage }

        val folderSuffix = if (sourceFile.isFolder) OCFile.PATH_SEPARATOR else ""
        val newPath = targetFile.remotePath + sourceFile.fileName + folderSuffix
        val newDocumentId = Document.fromPath(storageManager, newPath).documentId

        notifier.notifyChanged(targetFolder)
        return newDocumentId
    }

    @Throws(FileNotFoundException::class)
    fun move(sourceDocumentId: String, sourceParentDocumentId: String, targetParentDocumentId: String): String? {
        val targetFolder = resolver.toDocument(targetParentDocumentId)
        val targetFile = targetFolder.requireFile()
        if (!isValidTargetFolder(targetFile)) {
            return null
        }

        val document = resolver.toDocument(sourceDocumentId)
        val sourceFile = document.requireFile()
        if (!sourceFile.canMove()) {
            context.showToast(R.string.document_storage_provider_cannot_move)
            return null
        }

        awaitPendingUploads(document, sourceFile)
        MoveFileOperation(sourceFile.remotePath, targetFile.remotePath, document.storageManager)
            .execute(document.requireClient(context))
            .requireSuccess(TAG) {
                "Failed to move document with documentId $sourceDocumentId to $targetParentDocumentId"
            }

        notifier.notifyChanged(resolver.toDocument(sourceParentDocumentId))
        notifier.notifyChanged(targetFolder)
        return sourceDocumentId
    }

    /**
     * The upload starts asynchronously after close() and targets the remote path the document had then, so acting on
     * the server first would hit the stale copy and strand the upload. Large uploads can take hours, hence an idle
     * timeout instead of a total cap. close() also reaches the provider asynchronously, hence [CLOSE_GRACE_PERIOD].
     */
    @Throws(FileNotFoundException::class)
    private fun awaitPendingUploads(document: Document, file: OCFile) {
        val uploadHelper = fileUploadHelper.get()
        val accountName = document.user.accountName
        val waitStart = elapsedRealtime()
        var lastProgress = waitStart

        while (hasPendingUpload(document, file, uploadHelper, elapsedRealtime() - waitStart)) {
            lastProgress = maxOf(lastProgress, uploadHelper.getLastUploadProgress(accountName))
            if (elapsedRealtime() - lastProgress > PENDING_UPLOAD_IDLE_TIMEOUT.inWholeMilliseconds) {
                throw FileNotFoundException("Pending upload of ${file.remotePath} made no progress")
            }
            sleep(PENDING_UPLOAD_POLL_INTERVAL.inWholeMilliseconds)
        }
    }

    private fun hasPendingUpload(
        document: Document,
        file: OCFile,
        uploadHelper: FileUploadHelper,
        waitedMillis: Long
    ): Boolean {
        val writeStates = pendingWrites.states { isAffected(document, file, it) }
        return PendingWriteState.CLOSED_UPLOAD_NOT_QUEUED in writeStates ||
            (PendingWriteState.OPEN in writeStates && waitedMillis < CLOSE_GRACE_PERIOD.inWholeMilliseconds) ||
            uploadHelper.isUploadPending(file, document.user.accountName)
    }

    private fun isAffected(document: Document, file: OCFile, write: PendingDocumentWrite): Boolean =
        if (file.isFolder) {
            write.accountName == document.user.accountName &&
                document.storageManager.getFileById(write.fileId)?.remotePath?.startsWith(file.remotePath) == true
        } else {
            write.fileId == file.fileId
        }

    private fun isValidTargetFolder(targetFolder: OCFile): Boolean {
        isFolderPathValid = nameValidator.isValidFolderPath(targetFolder.fileName)
        if (!isFolderPathValid) {
            context.showToast(R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters)
        }
        return isFolderPathValid
    }

    companion object {
        private val TAG = DocumentEditor::class.java.simpleName
        private val PENDING_UPLOAD_IDLE_TIMEOUT = 10.minutes
        private val PENDING_UPLOAD_POLL_INTERVAL = 1.seconds
        private val CLOSE_GRACE_PERIOD = 5.seconds
    }
}
