/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.model

import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import android.content.ContentResolver
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import java.io.FileNotFoundException

internal class DocumentResolver {

    @Volatile
    private var rootIdToStorageManager: Map<String, FileDataStorageManager> = emptyMap()

    val storageManagers: Collection<FileDataStorageManager>
        get() = rootIdToStorageManager.values

    fun refresh(users: List<User>, contentResolver: ContentResolver) {
        rootIdToStorageManager = users.associateByTo(HashMap(), DocumentsStorageProvider::rootIdForUser) {
            FileDataStorageManager(it, contentResolver)
        }
    }

    fun storageManagerFor(rootId: String?): FileDataStorageManager? = rootIdToStorageManager[rootId]

    @Throws(FileNotFoundException::class)
    fun toDocument(documentId: String): Document {
        val (rootId, fileId) = splitDocumentId(documentId)
        val storageManager = rootIdToStorageManager[rootId]
            ?: throw FileNotFoundException("No storage manager associated for $documentId!")
        val parsedFileId = fileId.toLongOrNull() ?: throw FileNotFoundException("Invalid documentID $documentId!")
        return Document(storageManager, parsedFileId)
    }

    @Throws(FileNotFoundException::class)
    fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val parentDocument = toDocument(parentDocumentId)
        val parentFile = parentDocument.file ?: throw FileNotFoundException("No parent file with ID $parentDocumentId")

        val currentDocument = toDocument(documentId)
        val childFile = currentDocument.file ?: throw FileNotFoundException("No child file with ID $documentId")

        return parentDocument.user.nameEquals(currentDocument.user) &&
            childFile.decryptedRemotePath.startsWith(parentFile.decryptedRemotePath)
    }

    @Throws(FileNotFoundException::class)
    private fun splitDocumentId(documentId: String): List<String> =
        documentId.split(DocumentsStorageProvider.DOCUMENTID_SEPARATOR, limit = DOCUMENTID_PARTS)
            .takeIf { it.size == DOCUMENTID_PARTS }
            ?: throw FileNotFoundException("Invalid documentID $documentId!")

    companion object {
        private const val DOCUMENTID_PARTS = 2
    }
}
