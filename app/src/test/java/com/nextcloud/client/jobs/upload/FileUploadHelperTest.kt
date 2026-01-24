/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Raphael Vieira raphaelecv.projects@gmail.com
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.jobs.upload

import com.nextcloud.client.account.User
import com.nextcloud.client.di.AppComponent
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.UploadFileOperation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileUploadHelperTest {

    private lateinit var fileUploadHelper: FileUploadHelper

    @Before
    fun setUp() {
        mockkStatic(MainApp::class)
        val appComponent = mockk<AppComponent>(relaxed = true)
        every { MainApp.getAppComponent() } returns appComponent

        fileUploadHelper = FileUploadHelper()
        FileUploadWorker.activeUploadFileOperations.clear()
    }

    @After
    fun tearDown() {
        unmockkAll()
        FileUploadWorker.activeUploadFileOperations.clear()
    }

    @Test
    fun `isUploadingNow returns false for null upload`() {
        assertFalse(fileUploadHelper.isUploadingNow(null))
    }

    @Test
    fun `isUploadingNow returns false when no active operations`() {
        val upload = mockk<OCUpload>()
        every { upload.accountName } returns "account"
        every { upload.remotePath } returns "/file.txt"

        assertFalse(fileUploadHelper.isUploadingNow(upload))
    }

    @Test
    fun `isUploadingNow returns true when remotePath matches`() {
        val accountName = "account"
        val remotePath = "/file.txt"

        val upload = mockk<OCUpload>()
        every { upload.accountName } returns accountName
        every { upload.remotePath } returns remotePath

        val operation = mockk<UploadFileOperation>()
        val user = mockk<User>()
        every { user.accountName } returns accountName
        every { operation.user } returns user
        every { operation.remotePath } returns remotePath
        every { operation.oldFile } returns null

        FileUploadWorker.activeUploadFileOperations["key"] = operation

        assertTrue(fileUploadHelper.isUploadingNow(upload))
    }

    @Test
    fun `isUploadingNow returns true when old remotePath matches`() {
        val accountName = "account"
        val remotePath = "/file_renamed.txt"
        val oldRemotePath = "/file.txt"

        val upload = mockk<OCUpload>()
        every { upload.accountName } returns accountName
        every { upload.remotePath } returns oldRemotePath

        val operation = mockk<UploadFileOperation>()
        val user = mockk<User>()
        every { user.accountName } returns accountName
        every { operation.user } returns user
        every { operation.remotePath } returns remotePath

        val oldFile = mockk<OCFile>()
        every { oldFile.remotePath } returns oldRemotePath
        every { operation.oldFile } returns oldFile

        FileUploadWorker.activeUploadFileOperations["key"] = operation

        assertTrue(fileUploadHelper.isUploadingNow(upload))
    }

    @Test
    fun `isUploadingNow returns false when accountName does not match`() {
        val remotePath = "/file.txt"

        val upload = mockk<OCUpload>()
        every { upload.accountName } returns "account1"
        every { upload.remotePath } returns remotePath

        val operation = mockk<UploadFileOperation>()
        val user = mockk<User>()
        every { user.accountName } returns "account2"
        every { operation.user } returns user
        every { operation.remotePath } returns remotePath
        every { operation.oldFile } returns null

        FileUploadWorker.activeUploadFileOperations["key"] = operation

        assertFalse(fileUploadHelper.isUploadingNow(upload))
    }

    @Test
    fun `isUploadingNow returns false when paths do not match`() {
        val accountName = "account"

        val upload = mockk<OCUpload>()
        every { upload.accountName } returns accountName
        every { upload.remotePath } returns "/other.txt"

        val operation = mockk<UploadFileOperation>()
        val user = mockk<User>()
        every { user.accountName } returns accountName
        every { operation.user } returns user
        every { operation.remotePath } returns "/file.txt"
        every { operation.oldFile } returns null

        FileUploadWorker.activeUploadFileOperations["key"] = operation

        assertFalse(fileUploadHelper.isUploadingNow(upload))
    }
}
