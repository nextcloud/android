/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.text.TextUtils
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.account.User
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.jobs.offlineOperations.repository.OfflineOperationsRepository
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.providers.FileContentProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Callable

@Suppress("TooManyFunctions")
class FileStorageManagerTests {

    companion object {
        private const val ACCOUNT_NAME = "user@nextcloud.example.com"
        private const val AUTHORITY = "org.nextcloud"
        private const val FOLDER_ID = 3L
        private const val OTHER_FOLDER_ID = 4L
        private const val FOLDER_PATH = "/folder/"
        private const val FILE_PATH = "/folder/file.txt"
        private const val REMOVED_FILE_PATH = "/folder/removed.txt"
        private const val UNKNOWN_FILE_ID = -1L
        private const val EXISTING_ROW_ID = 7L
        private const val REMOVED_ROW_ID = 8L
        private const val NEW_ROW_ID = 42L
        private const val TRANSACTION = "transaction"
        private const val BATCH = "batch"
    }

    private lateinit var fileDao: FileDao
    private lateinit var database: NextcloudDatabase
    private lateinit var writableDatabase: SupportSQLiteDatabase
    private lateinit var contentResolver: ContentResolver
    private lateinit var storageManager: FileDataStorageManager
    private val events = mutableListOf<String>()
    private val deleteBuilder = operationBuilder()
    private val updateBuilder = operationBuilder()
    private val folder = OCFile(FOLDER_PATH).apply { fileId = FOLDER_ID }

    @Before
    fun setUp() {
        fileDao = mockk(relaxed = true)
        every { fileDao.getFileById(any()) } returns null
        every { fileDao.getFileByEncryptedRemotePath(any(), any()) } returns null

        writableDatabase = mockk(relaxed = true)
        database = mockk(relaxed = true)
        every { database.openHelper.writableDatabase } returns writableDatabase
        every { database.fileDao() } returns fileDao
        runTransactionsImmediately()

        contentResolver = mockk(relaxed = true)
        every { contentResolver.applyBatch(any(), any()) } answers {
            events.add(BATCH)
            emptyArray()
        }

        mockkStatic(MainApp::class)
        every { MainApp.getAppContext() } returns mockk(relaxed = true)
        every { MainApp.getAuthority() } returns AUTHORITY
        mockkObject(NextcloudDatabase.Companion)
        every { NextcloudDatabase.getInstance(any()) } returns database
        every { NextcloudDatabase.instance() } returns database
        mockkConstructor(OfflineOperationsRepository::class)
        mockkConstructor(ContentValues::class)
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { arg<CharSequence?>(0).isNullOrEmpty() }
        mockkStatic(ContentUris::class)
        every { ContentUris.withAppendedId(any(), any()) } returns mockk()
        mockkStatic(ContentProviderOperation::class)
        every { ContentProviderOperation.newDelete(any()) } returns deleteBuilder
        every { ContentProviderOperation.newUpdate(any()) } returns updateBuilder

        val user = mockk<User>(relaxed = true)
        every { user.accountName } returns ACCOUNT_NAME
        storageManager = FileDataStorageManager(user, contentResolver)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun operationBuilder(): ContentProviderOperation.Builder = mockk {
        every { withSelection(any(), any()) } returns this
        every { withValues(any()) } returns this
        every { build() } returns mockk()
    }

    private fun runTransactionsImmediately() {
        every { database.runInTransaction(any<Runnable>()) } answers {
            events.add(TRANSACTION)
            firstArg<Runnable>().run()
        }
        every { database.runInTransaction(any<Callable<Any?>>()) } answers {
            events.add(TRANSACTION)
            firstArg<Callable<Any?>>().call()
        }
    }

    private fun skipTransactions() {
        every { database.runInTransaction(any<Runnable>()) } returns Unit
        every { database.runInTransaction(any<Callable<Any?>>()) } returns null
    }

    private fun existingRow(): FileEntity = mockk { every { id } returns EXISTING_ROW_ID }

    private fun stubInsert() {
        every {
            writableDatabase.insert(ProviderTableMeta.FILE_TABLE_NAME, SQLiteDatabase.CONFLICT_NONE, any())
        } returns NEW_ROW_ID
    }

    private fun saveFolder(updatedFiles: List<OCFile> = emptyList(), filesToRemove: List<OCFile> = emptyList()) {
        storageManager.saveFolder(folder, updatedFiles, filesToRemove)
    }

    private fun verifyUpdatedRow(rowId: Long) {
        verify(exactly = 1) {
            writableDatabase.update(
                ProviderTableMeta.FILE_TABLE_NAME,
                SQLiteDatabase.CONFLICT_NONE,
                any(),
                "${ProviderTableMeta._ID} = ?",
                match { it.contentEquals(arrayOf<Any?>(rowId)) }
            )
        }
    }

    @Test
    fun `saveFolder inserts file and assigns generated id when no row matches id or path`() {
        stubInsert()
        val file = OCFile(FILE_PATH)

        saveFolder(updatedFiles = listOf(file))

        assertEquals(NEW_ROW_ID, file.fileId)
        verify(exactly = 0) { writableDatabase.update(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFolder updates row found by file id without inserting`() {
        every { fileDao.getFileById(EXISTING_ROW_ID) } returns existingRow()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH).apply { fileId = EXISTING_ROW_ID }))

        verifyUpdatedRow(EXISTING_ROW_ID)
        verify(exactly = 0) { writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `saveFolder updates row found by remote path when file id is unknown`() {
        every { fileDao.getFileByEncryptedRemotePath(FILE_PATH, ACCOUNT_NAME) } returns existingRow()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH).apply { fileId = UNKNOWN_FILE_ID }))

        verifyUpdatedRow(EXISTING_ROW_ID)
        verify(exactly = 0) { writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `saveFolder updates instead of inserting when the same folder is saved twice`() {
        stubInsert()
        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH)))
        every { fileDao.getFileByEncryptedRemotePath(FILE_PATH, ACCOUNT_NAME) } returns existingRow()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH)))

        verify(exactly = 1) { writableDatabase.insert(any(), any(), any()) }
        verifyUpdatedRow(EXISTING_ROW_ID)
    }

    @Test
    fun `saveFolder stores children under the saved folder`() {
        stubInsert()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH)))

        verify { anyConstructed<ContentValues>().put(ProviderTableMeta.FILE_PARENT, FOLDER_ID) }
    }

    @Test
    fun `saveFolder reads and writes children only inside a transaction`() {
        skipTransactions()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH)))

        verify(exactly = 0) { fileDao.getFileById(any()) }
        verify(exactly = 0) { fileDao.getFileByEncryptedRemotePath(any(), any()) }
        verify(exactly = 0) { writableDatabase.insert(any(), any(), any()) }
        verify(exactly = 0) { writableDatabase.update(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFolder commits children before applying removals and folder update`() {
        stubInsert()

        saveFolder(updatedFiles = listOf(OCFile(FILE_PATH)))

        assertEquals(listOf(TRANSACTION, BATCH), events)
    }

    @Test
    fun `saveFolder deletes only removed files whose parent is the saved folder`() {
        val removedChild = OCFile(REMOVED_FILE_PATH).apply {
            fileId = REMOVED_ROW_ID
            parentId = FOLDER_ID
        }
        val fileOfOtherFolder = OCFile("/other/removed.txt").apply { parentId = OTHER_FOLDER_ID }

        saveFolder(filesToRemove = listOf(removedChild, fileOfOtherFolder))

        verify(exactly = 1) { ContentProviderOperation.newDelete(any()) }
        verify(exactly = 1) { ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, REMOVED_ROW_ID) }
        verify(exactly = 1) {
            deleteBuilder.withSelection(
                "${ProviderTableMeta.FILE_ACCOUNT_OWNER} = ? AND ${ProviderTableMeta.FILE_PATH} = ?",
                match { it.contentEquals(arrayOf(ACCOUNT_NAME, REMOVED_FILE_PATH)) }
            )
        }
        verify(exactly = 1) { contentResolver.applyBatch(AUTHORITY, match { it.size == 2 }) }
    }

    @Test
    fun `saveFolder deletes local copy of a removed downloaded file`() {
        val localFile = Files.createTempFile("removed", ".txt").toFile()
        val removedChild = OCFile(REMOVED_FILE_PATH).apply {
            parentId = FOLDER_ID
            storagePath = localFile.absolutePath
            mimeType = "text/plain"
        }

        saveFolder(filesToRemove = listOf(removedChild))

        assertFalse(localFile.exists())
    }

    @Test
    fun `saveFolder updates folder row by its id`() {
        saveFolder()

        verify(exactly = 1) { ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI) }
        verify(exactly = 1) {
            updateBuilder.withSelection(
                "${ProviderTableMeta._ID} = ?",
                match { it.contentEquals(arrayOf(FOLDER_ID.toString())) }
            )
        }
    }

    @Test
    fun `deleteAllFiles deletes only rows of the current account`() {
        storageManager.deleteAllFiles()

        verify(exactly = 1) { fileDao.deleteAllFiles(ACCOUNT_NAME) }
        verify(exactly = 0) { contentResolver.delete(any(), any(), any()) }
    }

    @Test
    fun `upsertSingleFile updates existing row and never inserts when path already exists`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns EXISTING_ROW_ID
        every { writableDatabase.query(any<SimpleSQLiteQuery>()) } returns cursor
        val rowIds = captureAppendedRowIds()
        val values = mockk<ContentValues>(relaxed = true)

        FileContentProvider().upsertSingleFile(writableDatabase, ProviderTableMeta.CONTENT_URI_FILE, values)

        assertEquals(listOf(EXISTING_ROW_ID), rowIds)
        verify(exactly = 1) { writableDatabase.update(any(), any(), values, any(), any()) }
        verify(exactly = 0) { writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `upsertSingleFile inserts when no row matches path and owner`() {
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns false
        every { writableDatabase.query(any<SimpleSQLiteQuery>()) } returns cursor
        stubInsert()
        val rowIds = captureAppendedRowIds()

        FileContentProvider().upsertSingleFile(
            writableDatabase,
            ProviderTableMeta.CONTENT_URI_FILE,
            mockk(relaxed = true)
        )

        assertEquals(listOf(NEW_ROW_ID), rowIds)
        verify(exactly = 0) { writableDatabase.update(any(), any(), any(), any(), any()) }
    }

    private fun captureAppendedRowIds(): MutableList<Long> {
        val rowIds = mutableListOf<Long>()
        every { ContentUris.withAppendedId(any(), any()) } answers {
            rowIds.add(secondArg())
            mockk<Uri>()
        }
        return rowIds
    }
}
