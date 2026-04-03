/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 JARP <jarp@customer-187-174-218-184.uninet-ide.com.mx
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.test.RandomStringGenerator.make
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.operations.UploadFileOperation
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.Random
import java.util.UUID
import java.util.function.Supplier

/**
 * Created by JARP on 6/7/17.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class UploadStorageManagerTest : AbstractIT() {
    private lateinit var uploadsStorageManager: UploadsStorageManager

    @Mock
    private lateinit var currentAccountProvider: CurrentAccountProvider

    private lateinit var userAccountManager: UserAccountManager

    private lateinit var user2: User

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val instrumentationCtx = ApplicationProvider.getApplicationContext<Context>()
        val contentResolver = instrumentationCtx.contentResolver
        uploadsStorageManager = UploadsStorageManager(currentAccountProvider, contentResolver)
        userAccountManager = UserAccountManagerImpl.fromContext(targetContext)

        val temp = Account("test2@test.com", MainApp.getAccountType(targetContext))
        if (!userAccountManager.exists(temp)) {
            val platformAccountManager = AccountManager.get(targetContext)
            platformAccountManager.addAccountExplicitly(temp, "testPassword", null)
            platformAccountManager.setUserData(
                temp,
                AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                UserAccountManager.ACCOUNT_VERSION.toString()
            )
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0")
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "test.com")
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "test") // same as userId
        }

        val userAccountManager: UserAccountManager = UserAccountManagerImpl.fromContext(targetContext)
        user2 = userAccountManager.getUser("test2@test.com")
            .orElseThrow(Supplier { ActivityNotFoundException() })
    }

    @Test
    fun testDeleteAllUploads() {
        // Clean
        for (user in userAccountManager.getAllUsers()) {
            uploadsStorageManager.removeUserUploads(user)
        }
        val accountRowsA = 3
        val accountRowsB = 4
        insertUploads(account, accountRowsA)
        insertUploads(user2.toPlatformAccount(), accountRowsB)

        Assert.assertEquals(
            "Expected 4 removed uploads files",
            4,
            uploadsStorageManager.removeUserUploads(user2).toLong()
        )
    }

    @Test
    fun largeTest() {
        val size = 3000
        val uploads = ArrayList<OCUpload?>()

        deleteAllUploads()
        Assert.assertEquals(0, uploadsStorageManager.getAllStoredUploads().size.toLong())

        for (i in 0..<size) {
            val upload = createUpload(account)

            uploads.add(upload)
            uploadsStorageManager.uploadDao.insertOrReplace(upload.toUploadEntity())
        }

        val storedUploads = uploadsStorageManager.getAllStoredUploads()
        Assert.assertEquals(size.toLong(), storedUploads.size.toLong())
        Assert.assertEquals(uploads.size.toLong(), storedUploads.size.toLong())

        for (i in 0..<size) {
            Assert.assertTrue(
                "Upload " + (i + 1) + "/" + size + " not found in stored uploads: " + storedUploads[i].localPath,
                contains(uploads, storedUploads[i])
            )
        }
    }

    @Test
    fun testIsSame() {
        val upload1 = OCUpload("/test", "/test", account.name)
        upload1.isUseWifiOnly = true
        val upload2 = OCUpload("/test", "/test", account.name)
        upload2.isUseWifiOnly = true

        Assert.assertTrue(upload1.isSame(upload2))

        upload2.isUseWifiOnly = false
        Assert.assertFalse(upload1.isSame(upload2))

        Assert.assertFalse(upload1.isSame(null))
        Assert.assertFalse(upload1.isSame(OCFile("/test")))
    }

    private fun contains(uploads: ArrayList<OCUpload?>, storedUpload: OCUpload): Boolean {
        for (i in uploads.indices) {
            if (storedUpload.isSame(uploads.get(i), true)) {
                return true
            }
        }
        return false
    }

    @Test(expected = NullPointerException::class)
    fun corruptedUpload() {
        val corruptUpload = OCUpload(
            File.separator + "LocalPath",
            OCFile.PATH_SEPARATOR + "RemotePath",
            account.name
        )

        corruptUpload.localPath = null
        uploadsStorageManager.uploadDao.insertOrReplace(corruptUpload.toUploadEntity())
        uploadsStorageManager.getAllStoredUploads()
    }

    @Test
    fun getById() {
        val upload = createUpload(account)
        val id = uploadsStorageManager.uploadDao.insertOrReplace(upload.toUploadEntity())
        val newUpload = uploadsStorageManager.getUploadById(id)

        Assert.assertNotNull(newUpload)
        Assert.assertEquals(upload.localAction.toLong(), newUpload!!.localAction.toLong())
        Assert.assertEquals(upload.folderUnlockToken, newUpload.folderUnlockToken)
    }

    @Test
    fun getByIdNull() {
        val newUpload = uploadsStorageManager.getUploadById(-1)
        Assert.assertNull(newUpload)
    }

    private fun insertUploads(account: Account, rowsToInsert: Int) {
        for (i in 0..<rowsToInsert) {
            uploadsStorageManager.uploadDao.insertOrReplace(createUpload(account).toUploadEntity())
        }
    }

    fun generateUniqueNumber(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString()
    }

    private fun createUpload(account: Account): OCUpload {
        val upload = OCUpload(
            File.separator + "very long long long long long long long long long long long " +
                "long long long long long long long long long long long long long long " +
                "long long long long long long long long long long long long long long " +
                "long long long long long long long LocalPath " +
                generateUniqueNumber(),
            OCFile.PATH_SEPARATOR + "very long long long long long long long long long " +
                "long long long long long long long long long long long long long long " +
                "long long long long long long long long long long long long long long " +
                "long long long long long long long long long long long long RemotePath " +
                generateUniqueNumber(),
            account.name
        )

        upload.fileSize = (Random().nextInt(20000) * 10000).toLong()
        upload.setUploadStatus(UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS)
        upload.localAction = 2
        upload.nameCollisionPolicy = NameCollisionPolicy.ASK_USER
        upload.isCreateRemoteFolder = false
        upload.uploadEndTimestamp = System.currentTimeMillis()
        upload.lastResult = UploadResult.DELAYED_FOR_WIFI
        upload.createdBy = UploadFileOperation.CREATED_BY_USER
        upload.isUseWifiOnly = true
        upload.isWhileChargingOnly = false
        upload.folderUnlockToken = make(10)

        return upload
    }

    private fun deleteAllUploads() {
        uploadsStorageManager.removeAllUploads()

        Assert.assertEquals(0, uploadsStorageManager.getAllStoredUploads().size.toLong())
    }

    @After
    fun tearDown() {
        deleteAllUploads()
        userAccountManager.removeUser(user2)
    }
}
