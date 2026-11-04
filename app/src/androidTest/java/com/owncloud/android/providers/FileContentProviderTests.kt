/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.providers

import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.typeConverter.OfflineOperationTypeConverter
import com.nextcloud.utils.extensions.toFileEntity
import com.nextcloud.utils.extensions.toFolderMetadataUpdate
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.utils.MimeType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber", "TooManyFunctions")
class FileContentProviderTests {

    companion object {
        private const val OWNER = "owner@example.com"
        private const val OTHER_OWNER = "other@example.com"
        private const val NO_PARENT = 0L
    }

    private lateinit var provider: FileContentProvider
    private lateinit var db: SupportSQLiteDatabase
    private var inMemoryDatabase: NextcloudDatabase? = null

    @Before
    fun setup() {
        provider = FileContentProvider()
        db = mockk()
    }

    @After
    fun tearDown() {
        inMemoryDatabase?.close()
    }

    @Test
    fun insertNewFileShouldReturnNewId() {
        val values = ContentValues().apply {
            put(ProviderMeta.ProviderTableMeta.FILE_PATH, "/path/to/file.txt")
            put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, "user@example.com")
        }

        val cursor = mockk<android.database.Cursor>()
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } just Runs
        every { db.query(any<SimpleSQLiteQuery>()) } returns cursor

        // Mock insert to return new ID
        every { db.insert(any(), any(), any()) } returns 42L

        val result: Uri = provider.upsertSingleFile(
            db,
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE,
            values
        )

        assertEquals(
            ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 42),
            result
        )
    }

    @Test
    fun updateExistingFileShouldReturnSameId() {
        val values = ContentValues().apply {
            put(ProviderMeta.ProviderTableMeta.FILE_PATH, "/path/to/file.txt")
            put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, "user@example.com")
        }

        // Simulate update returning 1 row affected
        every {
            db.update(
                ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                any(),
                values,
                any(),
                any()
            )
        } returns 1

        // Mock cursor to return ID 99
        val cursor = mockk<android.database.Cursor>()
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns 99L
        every { cursor.close() } just Runs

        every { db.query(any<SimpleSQLiteQuery>()) } returns cursor

        val result: Uri = provider.upsertSingleFile(
            db,
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE,
            values
        )

        assertEquals(ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 99), result)

        cursor.close()
    }

    private fun createProviderWithInMemoryDatabase(): FileContentProvider {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, NextcloudDatabase::class.java)
            .addTypeConverter(OfflineOperationTypeConverter())
            .allowMainThreadQueries()
            .build()
        inMemoryDatabase = database

        val provider = FileContentProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = context.getString(R.string.authority) })
        FileContentProvider::class.java.getDeclaredField("mDbHelper")
            .apply { isAccessible = true }
            .set(provider, database.openHelper)
        return provider
    }

    private fun FileContentProvider.insertFile(
        path: String,
        parentId: Long,
        owner: String = OWNER,
        isFolder: Boolean = false
    ): Long {
        val values = ContentValues().apply {
            put(ProviderTableMeta.FILE_PATH, path)
            put(ProviderTableMeta.FILE_ACCOUNT_OWNER, owner)
            put(ProviderTableMeta.FILE_PARENT, parentId)
            put(ProviderTableMeta.FILE_CONTENT_TYPE, if (isFolder) MimeType.DIRECTORY else "text/plain")
        }
        val uri = insert(ProviderTableMeta.CONTENT_URI_FILE, values)
        return ContentUris.parseId(requireNotNull(uri))
    }

    private fun storedPaths(owner: String = OWNER): List<String?> =
        requireNotNull(inMemoryDatabase).fileDao().getAllFiles(owner).map { it.path }

    @Test
    fun queryReturnsChildrenOfDirectoryWhenNoSelectionIsGiven() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        provider.insertFile("/a.txt", rootId)
        provider.insertFile("/b.txt", rootId)

        val cursor = provider.query(
            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, rootId),
            null,
            null,
            null,
            null
        )

        assertEquals(2, requireNotNull(cursor).use { it.count })
    }

    @Test
    fun deletingDirectoryRemovesAllOfItsDescendants() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        val folderId = provider.insertFile("/folder/", rootId, isFolder = true)
        val subFolderId = provider.insertFile("/folder/sub/", folderId, isFolder = true)
        provider.insertFile("/folder/a.txt", folderId)
        provider.insertFile("/folder/sub/b.txt", subFolderId)

        provider.delete(ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, folderId), null, null)

        assertEquals(listOf("/"), storedPaths())
    }

    @Test
    fun deletingDirectoriesWithoutIdRemovesOnlyMatchingDirectoriesAndTheirDescendants() {
        val provider = createProviderWithInMemoryDatabase()
        val ownerRootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        provider.insertFile("/a.txt", ownerRootId)
        val otherRootId = provider.insertFile("/", NO_PARENT, owner = OTHER_OWNER, isFolder = true)
        provider.insertFile("/b.txt", otherRootId, owner = OTHER_OWNER)

        provider.delete(
            ProviderTableMeta.CONTENT_URI_DIR,
            "${ProviderTableMeta.FILE_ACCOUNT_OWNER} = ? AND ${ProviderTableMeta.FILE_PATH} = ?",
            arrayOf(OWNER, "/")
        )

        assertTrue(storedPaths().isEmpty())
        assertEquals(listOf("/", "/b.txt"), storedPaths(OTHER_OWNER).sortedBy { it })
    }

    @Test
    fun insertingExistingPathUpdatesRowInsteadOfAddingDuplicate() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        val firstId = provider.insertFile("/a.txt", rootId)

        val secondId = provider.insertFile("/a.txt", rootId)

        assertEquals(firstId, secondId)
        assertEquals(listOf("/", "/a.txt"), storedPaths().sortedBy { it })
    }

    @Test
    fun fileDaoDeleteAllFilesRemovesOnlyRowsOfGivenOwner() {
        val provider = createProviderWithInMemoryDatabase()
        provider.insertFile("/", NO_PARENT, isFolder = true)
        provider.insertFile("/", NO_PARENT, owner = OTHER_OWNER, isFolder = true)

        requireNotNull(inMemoryDatabase).fileDao().deleteAllFiles(OWNER)

        assertTrue(storedPaths().isEmpty())
        assertEquals(listOf("/"), storedPaths(OTHER_OWNER))
    }

    @Test
    fun fileDaoUpsertInsertsNewRowAndUpdatesExistingRowInPlace() {
        createProviderWithInMemoryDatabase()
        val fileDao = requireNotNull(inMemoryDatabase).fileDao()
        val file = OCFile("/a.txt").apply { etag = "first" }

        val insertedId = fileDao.upsert(file.toFileEntity(OWNER))
        val updateResult = fileDao.upsert(file.apply { etag = "second" }.toFileEntity(OWNER).copy(id = insertedId))

        assertEquals(-1L, updateResult)
        assertEquals(listOf("/a.txt"), storedPaths())
        assertEquals("second", fileDao.getFileById(insertedId)?.etag)
    }

    @Test
    fun fileDaoDeleteFolderRemovesFolderAndAllDescendants() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        val folderId = provider.insertFile("/folder/", rootId, isFolder = true)
        val subFolderId = provider.insertFile("/folder/sub/", folderId, isFolder = true)
        provider.insertFile("/folder/a.txt", folderId)
        provider.insertFile("/folder/sub/b.txt", subFolderId)

        requireNotNull(inMemoryDatabase).fileDao().deleteFolder(folderId, OWNER, "/folder/")

        assertEquals(listOf("/"), storedPaths())
    }

    @Test
    fun fileDaoDeleteFolderKeepsFolderRowWhenPathDoesNotMatch() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        val folderId = provider.insertFile("/folder/", rootId, isFolder = true)
        provider.insertFile("/folder/a.txt", folderId)

        requireNotNull(inMemoryDatabase).fileDao().deleteFolder(folderId, OWNER, "/other/")

        assertEquals(listOf("/", "/folder/"), storedPaths().sortedBy { it })
    }

    @Test
    fun fileDaoDeleteFileRemovesOnlyRowMatchingIdOwnerAndPath() {
        val provider = createProviderWithInMemoryDatabase()
        val rootId = provider.insertFile("/", NO_PARENT, isFolder = true)
        val fileId = provider.insertFile("/a.txt", rootId)
        provider.insertFile("/b.txt", rootId)
        val fileDao = requireNotNull(inMemoryDatabase).fileDao()

        fileDao.deleteFile(fileId, OTHER_OWNER, "/a.txt")
        fileDao.deleteFile(fileId, OWNER, "/b.txt")
        assertEquals(listOf("/", "/a.txt", "/b.txt"), storedPaths().sortedBy { it })

        fileDao.deleteFile(fileId, OWNER, "/a.txt")
        assertEquals(listOf("/", "/b.txt"), storedPaths().sortedBy { it })
    }

    @Test
    fun fileDaoUpdateFolderMetadataKeepsFileOnlyColumns() {
        createProviderWithInMemoryDatabase()
        val fileDao = requireNotNull(inMemoryDatabase).fileDao()
        val storedFolder = OCFile("/folder/").apply {
            setFolder()
            etag = "old"
            storagePath = "/storage/folder"
            setE2eCounter(5L)
            etagInConflict = "conflict"
        }
        val folderId = fileDao.upsert(storedFolder.toFileEntity(OWNER))
        val serverFolder = OCFile("/folder/").apply {
            setFolder()
            fileId = folderId
            etag = "new"
        }

        fileDao.updateFolderMetadata(serverFolder.toFolderMetadataUpdate(OWNER))

        val updatedFolder = requireNotNull(fileDao.getFileById(folderId))
        assertEquals("new", updatedFolder.etag)
        assertEquals(0L, updatedFolder.contentLength)
        assertEquals(storedFolder.storagePath, updatedFolder.storagePath)
        assertEquals(5L, updatedFolder.e2eCounter)
        assertEquals("conflict", updatedFolder.etagInConflict)
    }
}
