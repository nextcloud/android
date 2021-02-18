package com.owncloud.android.providers

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
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.providers.DocumentsStorageProvider.DOCUMENTID_SEPARATOR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume

// Uploads can sometimes take a bit of time, so 15sec is still considered recent enough
private const val RECENT_MILLISECONDS = 15_000

object DocumentsProviderUtils {

    internal fun DocumentFile.getOCFile(storageManager: FileDataStorageManager): OCFile? {
        val id = getDocumentId(uri)
        val separated: List<String> = id.split(DOCUMENTID_SEPARATOR.toRegex())
        return storageManager.getFileById(separated[1].toLong())
    }

    internal fun DocumentFile.assertRegularFile(
        name: String? = null,
        size: Long? = null,
        mimeType: String? = null,
        parent: DocumentFile? = null
    ) {
        name?.let { assertEquals(it, this.name) }
        assertTrue(exists())
        assertTrue(isFile)
        assertFalse(isDirectory)
        assertFalse(isVirtual)
        size?.let { assertEquals(it, length()) }
        mimeType?.let { assertEquals(it, type) }
        parent?.let { assertEquals(it.uri.toString(), parentFile!!.uri.toString()) }
    }

    internal fun DocumentFile.assertRegularFolder(name: String? = null, parent: DocumentFile? = null) {
        name?.let { assertEquals(it, this.name) }
        assertTrue(exists())
        assertFalse(isFile)
        assertTrue(isDirectory)
        assertFalse(isVirtual)
        parent?.let { assertEquals(it.uri.toString(), parentFile!!.uri.toString()) }
    }

    internal fun DocumentFile.assertRecentlyModified() {
        val diff = System.currentTimeMillis() - lastModified()
        assertTrue("File $name older than expected: $diff", diff < RECENT_MILLISECONDS)
    }

    internal fun assertExistsOnServer(client: OwnCloudClient, remotePath: String, shouldExit: Boolean) {
        val result = ExistenceCheckRemoteOperation(remotePath, !shouldExit).execute(client)
        assertTrue("$result", result.isSuccess)
    }

    internal fun assertListFilesEquals(expected: Collection<DocumentFile>, actual: Collection<DocumentFile>) {
//        assertEquals(
//            "Actual: ${actual.map { it.name.toString() }}",
//            expected.map { it.uri.toString() }.apply { sorted() },
//            actual.map { it.uri.toString() }.apply { sorted() },
//        )
        // FIXME replace with commented out stronger assertion above
        //  when parallel [UploadFileOperation]s don't bring back deleted files
        val expectedSet = HashSet<String>(expected.map { it.uri.toString() })
        val actualSet = HashSet<String>(actual.map { it.uri.toString() })
        assertTrue(actualSet.containsAll(expectedSet))
        actualSet.removeAll(expectedSet)
        actualSet.forEach {
            Log.e("TEST", "Error: Found unexpected file: $it")
        }
    }

    internal fun assertReadEquals(data: ByteArray, inputStream: InputStream?) {
        assertNotNull(inputStream)
        inputStream!!.use {
            assertArrayEquals(data, it.readBytes())
        }
    }

    /**
     * Same as [DocumentFile.findFile] only that it re-queries when the first result was stale.
     *
     * Most documents providers including Nextcloud are listing the full directory content
     * when querying for a specific file in a directory,
     * so there is no point in trying to optimize the query by not listing all children.
     */
    suspend fun DocumentFile.findFileBlocking(context: Context, displayName: String): DocumentFile? {
        val files = listFilesBlocking(context)
        for (doc in files) {
            if (displayName == doc.name) return doc
        }
        return null
    }

    /**
     * Works like [DocumentFile.listFiles] except that it waits until the DocumentProvider has a result.
     * This prevents getting an empty list even though there are children to be listed.
     */
    suspend fun DocumentFile.listFilesBlocking(context: Context) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val childrenUri = buildChildDocumentsUriUsingTree(uri, getDocumentId(uri))
        val projection = arrayOf(COLUMN_DOCUMENT_ID, COLUMN_MIME_TYPE)
        val result = ArrayList<DocumentFile>()

        try {
            getLoadedCursor {
                resolver.query(childrenUri, projection, null, null, null)
            }
        } catch (e: TimeoutCancellationException) {
            throw IOException(e)
        }.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val isDirectory = cursor.getString(1) == MIME_TYPE_DIR
                val file = if (isDirectory) {
                    val treeUri = buildTreeDocumentUri(uri.authority, documentId)
                    DocumentFile.fromTreeUri(context, treeUri)!!
                } else {
                    val documentUri = buildDocumentUriUsingTree(uri, documentId)
                    DocumentFile.fromSingleUri(context, documentUri)!!
                }
                result.add(file)
            }
        }
        result
    }

    /**
     * Returns a cursor for the given query while ensuring that the cursor was loaded.
     *
     * When the SAF backend is a cloud storage provider (e.g. Nextcloud),
     * it can happen that the query returns an outdated (e.g. empty) cursor
     * which will only be updated in response to this query.
     *
     * See: https://commonsware.com/blog/2019/12/14/scoped-storage-stories-listfiles-woe.html
     *
     * This method uses a [suspendCancellableCoroutine] to wait for the result of a [ContentObserver]
     * registered on the cursor in case the cursor is still loading ([EXTRA_LOADING]).
     * If the cursor is not loading, it will be returned right away.
     *
     * @param timeout an optional time-out in milliseconds
     * @throws TimeoutCancellationException if there was no result before the time-out
     * @throws IOException if the query returns null
     */
    @Suppress("EXPERIMENTAL_API_USAGE")
    @VisibleForTesting
    internal suspend fun getLoadedCursor(timeout: Long = 15_000, query: () -> Cursor?) =
        withTimeout(timeout) {
            suspendCancellableCoroutine<Cursor> { cont ->
                val cursor = query() ?: throw IOException("Initial query returned no results")
                cont.invokeOnCancellation { cursor.close() }
                val loading = cursor.extras.getBoolean(EXTRA_LOADING, false)
                if (loading) {
                    Log.e("TEST", "Cursor was loading, wait for update...")
                    cursor.registerContentObserver(
                        object : ContentObserver(null) {
                            override fun onChange(selfChange: Boolean, uri: Uri?) {
                                cursor.close()
                                val newCursor = query()
                                if (newCursor == null) cont.cancel(IOException("Re-query returned no results"))
                                else cont.resume(newCursor)
                            }
                        }
                    )
                } else {
                    // not loading, return cursor right away
                    cont.resume(cursor)
                }
            }
        }
}
