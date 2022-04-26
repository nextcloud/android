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

import android.view.Menu
import androidx.appcompat.view.menu.MenuBuilder
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.TestActivity
import com.nextcloud.client.account.User
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FileMenuFilterIT : AbstractIT() {

    @Mock
    private lateinit var componentsGetter: ComponentsGetter

    @Mock
    private lateinit var fileDataStorageManager: FileDataStorageManager

    private lateinit var mocks: AutoCloseable

    @Before
    fun setup() {
        mocks = MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        mocks.close()
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
            ExpectedLockVisibilities(lockFile = false, unlockFile = false, lockedBy = false, lockedUntil = false)
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
            ExpectedLockVisibilities(lockFile = true, unlockFile = false, lockedBy = false, lockedUntil = false)
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
            ExpectedLockVisibilities(lockFile = false, unlockFile = true, lockedBy = true, lockedUntil = true)
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
            ExpectedLockVisibilities(lockFile = false, unlockFile = false, lockedBy = true, lockedUntil = true)
        )
    }

    private data class ExpectedLockVisibilities(
        val lockFile: Boolean,
        val unlockFile: Boolean,
        val lockedBy: Boolean,
        val lockedUntil: Boolean
    )

    private fun configureCapability(capability: OCCapability) {
        whenever(fileDataStorageManager.getCapability(any<User>())).thenReturn(capability)
        whenever(fileDataStorageManager.getCapability(any<String>())).thenReturn(capability)
        whenever(componentsGetter.storageManager).thenReturn(fileDataStorageManager)
    }

    private fun getMenu(activity: TestActivity): Menu {
        val inflater = activity.menuInflater
        val menu = MenuBuilder(activity)
        inflater.inflate(R.menu.item_file, menu)
        return menu
    }

    private fun testLockingVisibilities(
        capability: OCCapability,
        file: OCFile,
        expectedLockVisibilities: ExpectedLockVisibilities
    ) {
        configureCapability(capability)

        launchActivity<TestActivity>().use {
            it.onActivity { activity ->
                val menu = getMenu(activity)

                val sut = FileMenuFilter(file, componentsGetter, activity, true, user)

                sut.filter(menu, false)

                Assert.assertEquals(
                    expectedLockVisibilities.lockFile,
                    menu.findItem(R.id.action_lock_file).isVisible
                )
                Assert.assertEquals(
                    expectedLockVisibilities.unlockFile,
                    menu.findItem(R.id.action_unlock_file).isVisible
                )
                Assert.assertEquals(
                    expectedLockVisibilities.lockedBy,
                    menu.findItem(R.id.action_locked_by).isVisible
                )
                Assert.assertEquals(
                    expectedLockVisibilities.lockedUntil,
                    menu.findItem(R.id.action_locked_until).isVisible
                )

                // locked by and until should always be disabled, they're not real actions
                Assert.assertFalse(menu.findItem(R.id.action_locked_by).isEnabled)
                Assert.assertFalse(menu.findItem(R.id.action_locked_until).isEnabled)
            }
        }
    }
}
