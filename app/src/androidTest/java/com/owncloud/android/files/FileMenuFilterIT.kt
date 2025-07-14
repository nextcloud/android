/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.test.TestActivity
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
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
    private lateinit var mockFileUploaderBinder: FileUploadHelper

    @MockK
    private lateinit var mockFileDownloadProgressListener: FileDownloadWorker.FileDownloadProgressListener

    @MockK
    private lateinit var mockOperationsServiceBinder: OperationsService.OperationsServiceBinder

    @MockK
    private lateinit var mockArbitraryDataProvider: ArbitraryDataProvider

    private lateinit var editorUtils: EditorUtils

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { mockFileUploaderBinder.isUploading(any(), any()) } returns false
        every { mockComponentsGetter.fileUploaderHelper } returns mockFileUploaderBinder
        every { mockFileDownloadProgressListener.isDownloading(any(), any()) } returns false
        every { mockComponentsGetter.fileDownloadProgressListener } returns mockFileDownloadProgressListener
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

    @Test
    fun filter_stream() {
        val capability = OCCapability().apply {
            endToEndEncryption = CapabilityBooleanType.TRUE
        }

        val encryptedVideo = OCFile("/e2e/1.mpg").apply {
            isEncrypted = true
            mimeType = "video/mpeg"
        }

        val normalVideo = OCFile("/folder/2.mpg").apply {
            mimeType = "video/mpeg"
            fileLength = SecureRandom().nextLong()
        }

        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory =
                    FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                var sut = filterFactory.newInstance(encryptedVideo, mockComponentsGetter, true, user)
                var toHide = sut.getToHide(false)

                // encrypted video, with content
                assertTrue(toHide.contains(R.id.action_stream_media))

                // regular video, with content
                sut = filterFactory.newInstance(normalVideo, mockComponentsGetter, true, user)
                toHide = sut.getToHide(false)

                assertFalse(toHide.contains(R.id.action_stream_media))
            }
        }
    }

    @Test
    fun filter_select_all() {
        configureCapability(OCCapability())

        // not in single file fragment -> multi selection is possible under certain circumstances

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory = FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                val files = listOf(OCFile("/foo.bin"), OCFile("/bar.bin"), OCFile("/baz.bin"))

                // single file, not in multi selection
                // *Select all* and *Deselect all* should stay hidden
                var sut = filterFactory.newInstance(files.first(), mockComponentsGetter, true, user)

                var toHide = sut.getToHide(false)
                assertTrue(toHide.contains(R.id.action_select_all_action_menu))
                assertTrue(toHide.contains(R.id.action_deselect_all_action_menu))

                // multiple files, all selected in multi selection
                // *Deselect all* shown, *Select all* not
                sut = filterFactory.newInstance(files.size, files, mockComponentsGetter, false, user)

                toHide = sut.getToHide(false)
                assertTrue(toHide.contains(R.id.action_select_all_action_menu))
                assertFalse(toHide.contains(R.id.action_deselect_all_action_menu))

                // multiple files, all but one selected
                // both *Select all* and *Deselect all* should be shown
                sut = filterFactory.newInstance(files.size + 1, files, mockComponentsGetter, false, user)

                toHide = sut.getToHide(false)
                assertFalse(toHide.contains(R.id.action_select_all_action_menu))
                assertFalse(toHide.contains(R.id.action_deselect_all_action_menu))
            }
        }
    }

    fun filter_select_all_singleFileFragment() {
        configureCapability(OCCapability())

        // in single file fragment (e.g. FileDetailFragment or PreviewImageFragment), selecting multiple files
        // is not possible -> *Select all* and *Deselect all* options should be hidden

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val filterFactory = FileMenuFilter.Factory(mockStorageManager, activity, editorUtils)

                val files = listOf(OCFile("/foo.bin"), OCFile("/bar.bin"), OCFile("/baz.bin"))

                // single file
                var sut = filterFactory.newInstance(files.first(), mockComponentsGetter, true, user)

                var toHide = sut.getToHide(true)
                assertTrue(toHide.contains(R.id.action_select_all_action_menu))
                assertTrue(toHide.contains(R.id.action_deselect_all_action_menu))

                // multiple files, all selected
                sut = filterFactory.newInstance(files.size, files, mockComponentsGetter, false, user)

                toHide = sut.getToHide(true)
                assertTrue(toHide.contains(R.id.action_select_all_action_menu))
                assertTrue(toHide.contains(R.id.action_deselect_all_action_menu))

                // multiple files, all but one selected
                sut = filterFactory.newInstance(files.size + 1, files, mockComponentsGetter, false, user)

                toHide = sut.getToHide(true)
                assertTrue(toHide.contains(R.id.action_select_all_action_menu))
                assertTrue(toHide.contains(R.id.action_deselect_all_action_menu))
            }
        }
    }

    private data class ExpectedLockVisibilities(val lockFile: Boolean, val unlockFile: Boolean)

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
