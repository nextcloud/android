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
import com.owncloud.android.providers.documentstorage.util.requireSuccess
import android.content.Context
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.CopyFileOperation
import com.owncloud.android.operations.MoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import java.io.FileNotFoundException

internal class DocumentEditor(
    private val context: Context,
    private val resolver: DocumentResolver,
    private val notifier: DocumentNotifier,
    private val folderRefresher: FolderRefresher,
    private val nameValidator: DocumentNameValidator
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

        MoveFileOperation(sourceFile.remotePath, targetFile.remotePath, document.storageManager)
            .execute(document.requireClient(context))
            .requireSuccess(TAG) {
                "Failed to move document with documentId $sourceDocumentId to $targetParentDocumentId"
            }

        notifier.notifyChanged(resolver.toDocument(sourceParentDocumentId))
        notifier.notifyChanged(targetFolder)
        return sourceDocumentId
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
    }
}
