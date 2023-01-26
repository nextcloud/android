/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.files

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.account.User
import com.nextcloud.test.TestActivity
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.services.OperationsService
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.MimeType
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class FileMenuFilterIT : AbstractIT() {

    @MockK
    private lateinit var mockComponentsGetter: ComponentsGetter

    @MockK
    private lateinit var mockStorageManager: FileDataStorageManager

    @MockK
    private lateinit var mockFileUploaderBinder: FileUploader.FileUploaderBinder

    @MockK
    private lateinit var mockFileDownloaderBinder: FileDownloader.FileDownloaderBinder

    @MockK
    private lateinit var mockOperationsServiceBinder: OperationsService.OperationsServiceBinder

    @MockK
    private lateinit var mockArbitraryDataProvider: ArbitraryDataProvider

    private lateinit var editorUtils: EditorUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { mockFileUploaderBinder.isUploading(any(), any()) } returns false
        every { mockComponentsGetter.fileUploaderBinder } returns mockFileUploaderBinder
        every { mockFileDownloaderBinder.isDownloading(any(), any()) } returns false
        every { mockComponentsGetter.fileDownloaderBinder } returns mockFileDownloaderBinder
        every { mockOperationsServiceBinder.isSynchronizing(any(), any()) } returns false
        every { mockComponentsGetter.operationsServiceBinder } returns mockOperationsServiceBinder
        every { mockStorageManager.getFileById(any()) } returns OCFile("/")
        every { mockStorageManager.getFolderContent(any(), any()) } returns ArrayList<OCFile>()
        every { mockArbitraryDataProvider.getValue(any<User>(), any()) } returns ""
        editorUtils = EditorUtils(mockArbitraryDataProvider)
    }

    @Test
    fun filter_noLockingCapability_lockItemsInvisible() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.UNKNOWN
        }

        val file = OCFile("/foo.md")

        testLockingVisibilities(
            capability,
            file,
            ExpectedLockVisibilities(lockFile = false, unlockFile = false)
        )
    }

    @Test
    fun filter_lockingCapability_fileUnlocked_lockVisible() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.UNKNOWN
            filesLockingVersion = "1.0"
        }

        val file = OCFile("/foo.md")

        testLockingVisibilities(
            capability,
            file,
            ExpectedLockVisibilities(lockFile = true, unlockFile = false)
        )
    }

    @Test
    fun filter_lockingCapability_fileLocked_lockedByAndProps() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.UNKNOWN
            filesLockingVersion = "1.0"
        }

        val file = OCFile("/foo.md").apply {
            isLocked = true
            lockType = FileLockType.MANUAL
            lockOwnerId = user.accountName.split("@")[0]
            lockOwnerDisplayName = "TEST"
            lockTimestamp = 1000 // irrelevant
            lockTimeout = 1000 // irrelevant
        }

        testLockingVisibilities(
            capability,
            file,
            ExpectedLockVisibilities(lockFile = false, unlockFile = true)
        )
    }

    @Test
    fun filter_lockingCapability_fileLockedByOthers_lockedByAndProps() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.UNKNOWN
            filesLockingVersion = "1.0"
        }

        val file = OCFile("/foo.md").apply {
            isLocked = true
            lockType = FileLockType.MANUAL
            lockOwnerId = "A_DIFFERENT_USER"
            lockOwnerDisplayName = "A_DIFFERENT_USER"
            lockTimestamp = 1000 // irrelevant
            lockTimeout = 1000 // irrelevant
        }
        testLockingVisibilities(
            capability,
            file,
            ExpectedLockVisibilities(lockFile = false, unlockFile = false)
        )
    }

    @Test
    fun filter_unset_encryption() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.TRUE
        }

        val encryptedFolder = OCFile("/encryptedFolder/").apply {
            isEncrypted = true
            mimeType = MimeType.DIRECTORY
            fileLength = SecureRandom().nextLong()
        }

        val encryptedEmptyFolder = OCFile("/encryptedFolder/").apply {
            isEncrypted = true
            mimeType = MimeType.DIRECTORY
        }

        val normalFolder = OCFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            fileLength = SecureRandom().nextLong()
        }

        val normalEmptyFolder = OCFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
        }

        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory =
                    FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                var sut = filterFactory.newInstance(encryptedFolder, mockComponentsGetter, true, user)
                var toHide = sut.getToHide(false)

                // encrypted folder, with content
                assertTrue(toHide.contains(R.id.action_unset_encrypted))
                assertTrue(toHide.contains(R.id.action_encrypted))
                assertTrue(toHide.contains(R.id.action_remove_file))

                // encrypted, but empty folder
                sut = filterFactory.newInstance(encryptedEmptyFolder, mockComponentsGetter, true, user)
                toHide = sut.getToHide(false)

                assertTrue(toHide.contains(R.id.action_unset_encrypted))
                assertTrue(toHide.contains(R.id.action_remove_file))
                assertTrue(toHide.contains(R.id.action_encrypted))

                // regular folder, with content
                sut = filterFactory.newInstance(normalFolder, mockComponentsGetter, true, user)
                toHide = sut.getToHide(false)

                assertTrue(toHide.contains(R.id.action_unset_encrypted))
                assertTrue(toHide.contains(R.id.action_encrypted))
                assertFalse(toHide.contains(R.id.action_remove_file))

                // regular folder, without content
                sut = filterFactory.newInstance(normalEmptyFolder, mockComponentsGetter, true, user)
                toHide = sut.getToHide(false)

                assertTrue(toHide.contains(R.id.action_unset_encrypted))
                assertFalse(toHide.contains(R.id.action_encrypted))
                assertFalse(toHide.contains(R.id.action_remove_file))
            }
        }
    }

    private data class ExpectedLockVisibilities(
        val lockFile: Boolean,
        val unlockFile: Boolean
    )

    private fun configureCapability(capability: OCCapability) {
        every { mockStorageManager.getCapability(any<User>()) } returns capability
        every { mockStorageManager.getCapability(any<String>()) } returns capability
    }

    private fun testLockingVisibilities(
        capability: OCCapability,
        file: OCFile,
        expectedLockVisibilities: ExpectedLockVisibilities
    ) {
        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory =
                    FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)
                val sut = filterFactory.newInstance(file, mockComponentsGetter, true, user)

                val toHide = sut.getToHide(false)

                assertEquals(
                    expectedLockVisibilities.lockFile,
                    !toHide.contains(R.id.action_lock_file)
                )
                assertEquals(
                    expectedLockVisibilities.unlockFile,
                    !toHide.contains(R.id.action_unlock_file)
                )
            }
        }
    }
}
