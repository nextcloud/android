package com.owncloud.android.providers

import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile.ROOT_PATH
import com.owncloud.android.providers.DocumentsProviderUtils.assertExistsOnServer
import com.owncloud.android.providers.DocumentsProviderUtils.assertListFilesEquals
import com.owncloud.android.providers.DocumentsProviderUtils.assertReadEquals
import com.owncloud.android.providers.DocumentsProviderUtils.assertRecentlyModified
import com.owncloud.android.providers.DocumentsProviderUtils.assertRegularFile
import com.owncloud.android.providers.DocumentsProviderUtils.assertRegularFolder
import com.owncloud.android.providers.DocumentsProviderUtils.findFileBlocking
import com.owncloud.android.providers.DocumentsProviderUtils.getOCFile
import com.owncloud.android.providers.DocumentsProviderUtils.listFilesBlocking
import com.owncloud.android.providers.DocumentsStorageProvider.DOCUMENTID_SEPARATOR
import kotlinx.coroutines.runBlocking
import net.bytebuddy.utility.RandomString
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity
import org.apache.jackrabbit.webdav.client.methods.PutMethod
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private const val MAX_FILE_NAME_LENGTH = 225

class DocumentsStorageProviderIT : AbstractOnServerIT() {

    private val context = targetContext
    private val contentResolver = context.contentResolver
    private val authority = context.getString(R.string.document_provider_authority)

    private val rootFileId = storageManager.getFileByEncryptedRemotePath(ROOT_PATH).fileId
    private val documentId = "${user.hashCode()}${DOCUMENTID_SEPARATOR}$rootFileId"
    private val uri = DocumentsContract.buildTreeDocumentUri(authority, documentId)
    private val rootDir get() = DocumentFile.fromTreeUri(context, uri)!!

    @Before
    fun before() {
        // DocumentsProvider#onCreate() is called when the application is started
        // which is *after* AbstractOnServerIT adds the accounts (when the app is freshly installed).
        // So we need to query our roots here to ensure that the internal storage map is initialized.
        contentResolver.query(DocumentsContract.buildRootsUri(authority), null, null, null)
        assertTrue("Storage root does not exist", rootDir.exists())
        assertTrue(rootDir.isDirectory)
    }

    /**
     * Delete all files in [rootDir] after each test.
     *
     * We can't use [AbstractOnServerIT.after] as this is only deleting remote files.
     */
    @After
    override fun after() = runBlocking {
        rootDir.listFilesBlocking(context).forEach {
            Log.e("TEST", "Deleting ${it.name}...")
            it.delete()
        }
    }

    @Test
    fun testCreateDeleteFiles() = runBlocking {
        // no files in root initially
        assertListFilesEquals(emptyList(), rootDir.listFilesBlocking(context))

        // create first file
        val name1 = RandomString.make()
        val type1 = "text/html"
        val file1 = rootDir.createFile(type1, name1)!!

        // check assumptions
        @Suppress("ForbiddenComment")
        file1.assertRegularFile(name1, 0L, null/* FIXME: type1 */, rootDir)
        file1.assertRecentlyModified()

        // file1 is found in root
        assertListFilesEquals(listOf(file1), rootDir.listFilesBlocking(context).toList())

        // file1 was uploaded
        val ocFile1 = file1.getOCFile(storageManager)!!
        assertExistsOnServer(client, ocFile1.remotePath, true)

        // create second long file with long file name
        val name2 = RandomString.make(MAX_FILE_NAME_LENGTH)
        val type2 = "application/octet-stream"
        val file2 = rootDir.createFile(type2, name2)!!

        // file2 was uploaded
        val ocFile2 = file2.getOCFile(storageManager)!!
        assertExistsOnServer(client, ocFile2.remotePath, true)

        // check assumptions
        file2.assertRegularFile(name2, 0L, type2, rootDir)
        file2.assertRecentlyModified()

        // both files get listed in root
        assertListFilesEquals(listOf(file1, file2), rootDir.listFiles().toList())

        // delete first file
        assertTrue(file1.delete())
        assertFalse(file1.exists())
        assertExistsOnServer(client, ocFile1.remotePath, false)

        // only second file gets listed in root
        assertListFilesEquals(listOf(file2), rootDir.listFiles().toList())

        // delete also second file
        assertTrue(file2.delete())
        assertFalse(file2.exists())
        assertExistsOnServer(client, ocFile2.remotePath, false)

        // no more files in root
        assertListFilesEquals(emptyList(), rootDir.listFilesBlocking(context))
    }

    @Test
    fun testReadWriteFiles() {
        // create random file
        val file1 = rootDir.createFile("application/octet-stream", RandomString.make())!!
        file1.assertRegularFile(size = 0L)

        // write random bytes to file
        @Suppress("MagicNumber")
        val dataSize = Random.nextInt(1, 99) * 1024
        val data1 = Random.nextBytes(dataSize)
        contentResolver.openOutputStream(file1.uri, "wt").use {
            it!!.write(data1)
        }

        // read back random bytes
        assertReadEquals(data1, contentResolver.openInputStream(file1.uri))

        // file size was updated correctly
        file1.assertRegularFile(size = data1.size.toLong())
    }

    @Test
    fun testCreateDeleteFolders() = runBlocking {
        // create a new folder
        val dirName1 = RandomString.make()
        val dir1 = rootDir.createDirectory(dirName1)!!
        dir1.assertRegularFolder(dirName1, rootDir)
        // FIXME about a minute gets lost somewhere after CFO sets the correct time
        @Suppress("MagicNumber")
        assertTrue(System.currentTimeMillis() - dir1.lastModified() < 60_000)
//        dir1.assertRecentlyModified()

        // ensure folder was uploaded to server
        val ocDir1 = dir1.getOCFile(storageManager)!!
        assertExistsOnServer(client, ocDir1.remotePath, true)

        // create file in folder
        val file1 = dir1.createFile("text/html", RandomString.make())!!
        file1.assertRegularFile(parent = dir1)
        val ocFile1 = file1.getOCFile(storageManager)!!
        assertExistsOnServer(client, ocFile1.remotePath, true)

        // we find the new file in the created folder and get it in the list
        assertEquals(file1.uri.toString(), dir1.findFileBlocking(context, file1.name!!)!!.uri.toString())
        assertListFilesEquals(listOf(file1), dir1.listFilesBlocking(context))

        // delete folder
        dir1.delete()
        assertFalse(dir1.exists())
        assertExistsOnServer(client, ocDir1.remotePath, false)

        // ensure file got deleted with it
        assertFalse(file1.exists())
        assertExistsOnServer(client, ocFile1.remotePath, false)
    }

    @Test
    fun testServerChangedFileContent() {
        // create random file
        val file1 = rootDir.createFile("text/plain", RandomString.make())!!
        file1.assertRegularFile(size = 0L)

        val createdETag = file1.getOCFile(storageManager)!!.etagOnServer

        assertTrue(createdETag.isNotEmpty())

        val content1 = "initial content".toByteArray()

        // write content bytes to file
        contentResolver.openOutputStream(file1.uri, "wt").use {
            it!!.write(content1)
        }

        // refresh
        while (file1.getOCFile(storageManager)!!.etagOnServer == createdETag) {
            shortSleep()
            rootDir.listFiles()
        }

        val remotePath = file1.getOCFile(storageManager)!!.remotePath

        val content2 = "new content".toByteArray()

        // modify content on server side
        val putMethod = PutMethod(client.getFilesDavUri(remotePath))
        putMethod.requestEntity = ByteArrayRequestEntity(content2)
        assertEquals(HttpStatus.SC_NO_CONTENT, client.executeMethod(putMethod))
        client.exhaustResponse(putMethod.responseBodyAsStream)
        putMethod.releaseConnection() // let the connection available for other methods

        // read back content bytes
        val bytes = contentResolver.openInputStream(file1.uri)?.readBytes() ?: ByteArray(0)
        assertEquals(String(content2), String(bytes))
    }
}
