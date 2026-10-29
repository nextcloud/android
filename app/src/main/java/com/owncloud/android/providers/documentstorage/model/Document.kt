/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.model

import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import com.owncloud.android.providers.documentstorage.util.fileNotFoundException
import java.io.FileNotFoundException
import java.io.IOException

class Document(val storageManager: FileDataStorageManager, private val fileId: Long) {

    constructor(storageManager: FileDataStorageManager, file: OCFile) : this(storageManager, file.fileId)

    val documentId: String
        get() = DocumentsStorageProvider.rootIdForUser(user) + DocumentsStorageProvider.DOCUMENTID_SEPARATOR + fileId

    val user: User
        get() = storageManager.user

    val file: OCFile?
        get() = storageManager.getFileById(fileId)

    val parent: Document?
        get() = file?.parentId?.takeIf { it > 0 }?.let { Document(storageManager, it) }

    @Throws(FileNotFoundException::class)
    fun requireFile(): OCFile = file ?: throw FileNotFoundException("No file in database for documentId $documentId")

    @Throws(FileNotFoundException::class)
    fun requireClient(context: Context): OwnCloudClient = try {
        OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(user.toOwnCloudAccount(), context)
    } catch (e: Exception) {
        when (e) {
            is OperationCanceledException, is IOException, is AuthenticatorException -> {
                Log_OC.e(TAG, "Failed to set client", e)
                throw fileNotFoundException("Failed to get client for $documentId", e)
            }

            else -> throw e
        }
    }

    companion object {
        private val TAG = Document::class.java.simpleName

        @Throws(FileNotFoundException::class)
        fun fromPath(storageManager: FileDataStorageManager, remotePath: String): Document {
            val file = storageManager.getFileByEncryptedRemotePath(remotePath)
                ?: throw FileNotFoundException("No file in database for path $remotePath")
            return Document(storageManager, file)
        }
    }
}
