/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.media.MediaScannerConnection
import android.text.TextUtils
import com.nextcloud.client.account.User
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.jobs.offlineOperations.repository.OfflineOperationsRepository
import com.owncloud.android.MainApp
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before

@Suppress("TooManyFunctions", "DEPRECATION")
abstract class MoveFilesTestBase {

    companion object {
        const val ACCOUNT_NAME = "user@nextcloud.example.com"
        const val SAVE_PATH = "/storage/emulated/0/nextcloud/$ACCOUNT_NAME"
        const val OLD_PATH = "/documents/report.pdf"
        const val TARGET_PATH = "/archive/report.pdf"
        const val TARGET_PARENT_PATH = "/archive/"
    }

    lateinit var manager: FileDataStorageManager
    lateinit var mockFileDao: FileDao
    lateinit var mockUser: User

    @Before
    fun setUpBase() {
        mockFileDao = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        every { mockUser.accountName } returns ACCOUNT_NAME

        mockkStatic(MainApp::class)
        every { MainApp.getAppContext() } returns mockk(relaxed = true)

        val mockDb = mockk<NextcloudDatabase>(relaxed = true)
        mockkObject(NextcloudDatabase.Companion)
        every { NextcloudDatabase.getInstance(any()) } returns mockDb
        every { NextcloudDatabase.instance() } returns mockDb
        every { mockDb.fileDao() } returns mockFileDao
        every { mockDb.recommendedFileDao() } returns mockk(relaxed = true)
        every { mockDb.offlineOperationDao() } returns mockk(relaxed = true)
        every { mockDb.shareDao() } returns mockk(relaxed = true)
        every { mockDb.capabilityDao() } returns mockk(relaxed = true)

        mockkConstructor(OfflineOperationsRepository::class)

        mockkStatic(FileStorageUtils::class)
        every { FileStorageUtils.getSavePath(any()) } returns SAVE_PATH
        every { FileStorageUtils.getDefaultSavePathFor(any(), any()) } answers {
            SAVE_PATH + secondArg<OCFile>().remotePath
        }

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { arg<CharSequence?>(0).isNullOrEmpty() }

        mockkStatic(MediaScannerConnection::class)
        every { MediaScannerConnection.scanFile(any(), any(), any(), any()) } just runs

        val real = FileDataStorageManager(mockUser, null as ContentResolver?)
        manager = spyk(real)
        every { manager.getFileByPath(any()) } returns null
        every { manager.deleteFileInMediaScan(any()) } just runs
    }

    @After
    fun tearDownBase() {
        unmockkAll()
    }

    fun stubTargetParent(path: String = TARGET_PARENT_PATH, id: Long = 99L): OCFile {
        val parent = OCFile(path).apply {
            fileId = id
            mimeType = MimeType.DIRECTORY
        }
        every { manager.getFileByPath(path) } returns parent
        return parent
    }

    fun createFileEntity(
        id: Long = 1L,
        path: String,
        pathDecrypted: String? = path,
        storagePath: String? = null,
        contentType: String? = null,
        isEncrypted: Int? = 0,
        parent: Long? = null
    ): FileEntity = FileEntity(
        id = id,
        name = path.substringAfterLast("/"),
        encryptedName = null,
        path = path,
        pathDecrypted = pathDecrypted,
        parent = parent,
        creation = null,
        modified = null,
        contentType = contentType,
        contentLength = null,
        storagePath = storagePath,
        accountOwner = null,
        lastSyncDate = null,
        lastSyncDateForData = null,
        modifiedAtLastSyncForData = null,
        etag = null,
        etagOnServer = null,
        sharedViaLink = null,
        permissions = null,
        remoteId = null,
        localId = -1L,
        updateThumbnail = null,
        isDownloading = null,
        favorite = null,
        hidden = null,
        isEncrypted = isEncrypted,
        etagInConflict = null,
        sharedWithSharee = null,
        mountType = null,
        hasPreview = null,
        unreadCommentsCount = null,
        ownerId = null,
        ownerDisplayName = null,
        note = null,
        sharees = null,
        richWorkspace = null,
        metadataSize = null,
        metadataLivePhoto = null,
        locked = null,
        lockType = null,
        lockOwner = null,
        lockOwnerDisplayName = null,
        lockOwnerEditor = null,
        lockTimestamp = null,
        lockTimeout = null,
        lockToken = null,
        tags = null,
        metadataGPS = null,
        e2eCounter = null,
        internalTwoWaySync = null,
        internalTwoWaySyncResult = null,
        uploaded = null
    )
}
