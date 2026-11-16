/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import android.provider.DocumentsContract.EXTRA_LOADING
import android.provider.DocumentsContract.buildChildDocumentsUriUsingTree
import android.provider.DocumentsContract.buildDocumentUriUsingTree
import android.provider.DocumentsContract.buildTreeDocumentUri
import android.provider.DocumentsContract.getDocumentId
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider.Companion.DOCUMENTID_SEPARATOR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object TestDocumentsProviderUtils {

    private const val TAG = "DocumentsProviderUtils"
    private const val DOCUMENT_ID_COLUMN = 0
    private const val MIME_TYPE_COLUMN = 1
    private val RECENTLY_MODIFIED_TOLERANCE = 15.seconds
    private val CURSOR_LOAD_TIMEOUT = 15.seconds
    private val CHILD_PROJECTION = arrayOf(COLUMN_DOCUMENT_ID, COLUMN_MIME_TYPE)

    internal fun DocumentFile.findOCFile(storageManager: FileDataStorageManager): OCFile? {
        val fileId = getDocumentId(uri).split(DOCUMENTID_SEPARATOR)[1].toLong()
        return storageManager.getFileById(fileId)
    }

    internal fun DocumentFile.requireOCFile(storageManager: FileDataStorageManager): OCFile =
        checkNotNull(findOCFile(storageManager)) { "No stored file for $uri" }

    internal fun DocumentFile.requireNewFile(mimeType: String, displayName: String): DocumentFile =
        checkNotNull(createFile(mimeType, displayName)) { "Could not create file $displayName in $uri" }

    internal fun DocumentFile.requireNewDirectory(displayName: String): DocumentFile =
        checkNotNull(createDirectory(displayName)) { "Could not create folder $displayName in $uri" }

    internal fun DocumentFile.assertRegularFile(
        name: String? = null,
        size: Long? = null,
        mimeType: String? = null,
        parent: DocumentFile? = null
    ) {
        name?.let { assertEquals(it, this.name) }
        assertTrue("$uri does not exist", exists())
        assertTrue(isFile)
        assertFalse(isDirectory)
        assertFalse(isVirtual)
        size?.let { assertEquals(it, length()) }
        mimeType?.let { assertEquals(it, type) }
        parent?.let { assertEquals(it.uri.toString(), parentFile?.uri.toString()) }
    }

    internal fun DocumentFile.assertRegularFolder(name: String? = null, parent: DocumentFile? = null) {
        name?.let { assertEquals(it, this.name) }
        assertTrue("$uri does not exist", exists())
        assertFalse(isFile)
        assertTrue(isDirectory)
        assertFalse(isVirtual)
        parent?.let { assertEquals(it.uri.toString(), parentFile?.uri.toString()) }
    }

    internal fun DocumentFile.assertRecentlyModified(tolerance: Duration = RECENTLY_MODIFIED_TOLERANCE) {
        val age = (System.currentTimeMillis() - lastModified()).milliseconds
        assertTrue("$name was modified $age ago, expected less than $tolerance", age < tolerance)
    }

    internal fun assertExistsOnServer(client: OwnCloudClient, remotePath: String, shouldExist: Boolean) {
        val result = ExistenceCheckRemoteOperation(remotePath, !shouldExist).execute(client)
        assertTrue("$result", result.isSuccess)
    }

    internal fun assertContainsAllFiles(expected: Collection<DocumentFile>, actual: Collection<DocumentFile>) {
        val expectedUris = expected.map { it.uri.toString() }.toSet()
        val actualUris = actual.map { it.uri.toString() }.toSet()
        assertTrue("Missing ${expectedUris - actualUris} in $actualUris", actualUris.containsAll(expectedUris))
        (actualUris - expectedUris).forEach { Log_OC.w(TAG, "Found unexpected file: $it") }
    }

    internal suspend fun DocumentFile.findChild(context: Context, displayName: String): DocumentFile? =
        listChildren(context).firstOrNull { it.name == displayName }

    internal suspend fun DocumentFile.listChildren(context: Context): List<DocumentFile> = withContext(Dispatchers.IO) {
        val childrenUri = buildChildDocumentsUriUsingTree(uri, getDocumentId(uri))
        val cursor = try {
            queryUntilLoaded { context.contentResolver.query(childrenUri, CHILD_PROJECTION, null, null, null) }
        } catch (e: TimeoutCancellationException) {
            throw IOException("Children of $uri did not load", e)
        }

        cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(it.toDocumentFile(context, uri))
                }
            }
        }
    }

    private fun Cursor.toDocumentFile(context: Context, parentUri: Uri): DocumentFile {
        val documentId = getString(DOCUMENT_ID_COLUMN)
        val documentFile = if (getString(MIME_TYPE_COLUMN) == MIME_TYPE_DIR) {
            DocumentFile.fromTreeUri(context, buildTreeDocumentUri(parentUri.authority, documentId))
        } else {
            DocumentFile.fromSingleUri(context, buildDocumentUriUsingTree(parentUri, documentId))
        }
        return checkNotNull(documentFile) { "No document for $documentId" }
    }

    private suspend fun queryUntilLoaded(query: () -> Cursor?): Cursor = withTimeout(CURSOR_LOAD_TIMEOUT) {
        suspendCancellableCoroutine { continuation ->
            val cursor = query() ?: throw IOException("Initial query returned no results")
            continuation.invokeOnCancellation { cursor.close() }

            if (!cursor.isLoading()) {
                continuation.resume(cursor)
                return@suspendCancellableCoroutine
            }

            cursor.registerContentObserver(
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        if (!continuation.isActive) return

                        cursor.close()
                        val reloadedCursor = query()
                        if (reloadedCursor == null) {
                            continuation.cancel(IOException("Re-query returned no results"))
                        } else {
                            continuation.resume(reloadedCursor)
                        }
                    }
                }
            )
        }
    }

    private fun Cursor.isLoading(): Boolean = extras?.getBoolean(EXTRA_LOADING) == true
}
