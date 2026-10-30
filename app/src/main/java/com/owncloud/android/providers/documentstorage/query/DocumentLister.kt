/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.query

import android.content.Context
import android.provider.DocumentsContract
import androidx.core.os.bundleOf
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.providers.documentstorage.sync.DocumentNotifier
import com.owncloud.android.providers.documentstorage.sync.FolderRefresher
import com.owncloud.android.ui.helpers.FileOperationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.nextcloud.providers.cursors.FileCursor
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

internal class DocumentLister(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notifier: DocumentNotifier,
    private val folderRefresher: FolderRefresher
) {

    @Throws(FileNotFoundException::class)
    fun listChildren(parentFolder: Document, projection: Array<String>?): FileCursor {
        val parentFile = parentFolder.requireFile()
        val cursor = FileCursor(projection)

        if (!canListChildren(parentFolder, parentFile)) {
            return cursor
        }

        addReadableChildren(cursor, parentFolder.storageManager, parentFile)
        val isLoading = startReloadIfExpired(parentFolder, parentFile, cursor)

        return cursor.apply {
            extras = bundleOf(DocumentsContract.EXTRA_LOADING to isLoading)
            setNotificationUri(context.contentResolver, notifier.notifyUri(parentFolder))
        }
    }

    fun search(storageManager: FileDataStorageManager?, query: String, projection: Array<String>?): FileCursor {
        val cursor = FileCursor(projection)
        val root = storageManager?.getFileByEncryptedRemotePath(OCFile.ROOT_PATH) ?: return cursor
        addMatchingFiles(cursor, storageManager, root, query)
        return cursor
    }

    private fun canListChildren(folder: Document, folderFile: OCFile): Boolean {
        if (!folderFile.canRead()) {
            context.showToast(R.string.document_storage_provider_cannot_read)
            return false
        }

        if (folderFile.isEncrypted && !FileOperationsHelper.isEndToEndEncryptionSetup(context, folder.user)) {
            context.showToast(R.string.e2e_not_yet_setup)
            return false
        }

        return true
    }

    private fun addReadableChildren(cursor: FileCursor, storageManager: FileDataStorageManager, folderFile: OCFile) {
        storageManager.getFolderContent(folderFile, false).forEach { file ->
            if (!file.canRead()) {
                Log_OC.w(TAG, "Skipping file, doesn't have read permission. RemotePath: ${file.remotePath}")
                return@forEach
            }

            cursor.addFile(Document(storageManager, file), file)
        }
    }

    private fun startReloadIfExpired(folder: Document, folderFile: OCFile, cursor: FileCursor): Boolean {
        if (folderFile.lastSyncDateForData + CACHE_EXPIRATION >= System.currentTimeMillis()) {
            return false
        }

        val reloadJob = scope.launch {
            runCatching { folderRefresher.reload(folder) }
            if (isActive) {
                notifier.notifyChanged(folder)
            }
        }
        cursor.loadingTask = reloadJob
        return true
    }

    private fun addMatchingFiles(
        cursor: FileCursor,
        storageManager: FileDataStorageManager,
        folder: OCFile,
        query: String
    ) {
        storageManager.getFolderContent(folder, false).forEach { file ->
            when {
                file.isFolder -> addMatchingFiles(cursor, storageManager, file, query)
                file.fileName.contains(query) -> cursor.addFile(Document(storageManager, file), file)
            }
        }
    }

    companion object {
        private val TAG = DocumentLister::class.java.simpleName
        private val CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(1)
    }
}
