/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.device.BatteryStatus
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class OCFileListFragmentIT : AbstractOnServerIT() {
    companion object {
        val SECOND_IN_MILLIS = 1000L
        val RESULT_PER_PAGE = 50
    }

    @get:Rule
    val activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isInternetWalled(): Boolean {
            return false
        }

        override fun getConnectivity(): Connectivity {
            return Connectivity.CONNECTED_WIFI
        }
    }

    private val powerManagementServiceMock: PowerManagementService = object : PowerManagementService {
        override val isPowerSavingEnabled: Boolean
            get() = false

        override val isPowerSavingExclusionAvailable: Boolean
            get() = false

        override val battery: BatteryStatus
            get() = BatteryStatus()
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFile() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test.txt")
        val file = File("/sdcard/test.txt")
        file.createNewFile()

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFile() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test.txt")
        val file = File("/sdcard/test.txt")
        file.writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithoutLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")
        val file = File("/sdcard/test/")
        File("/sdcard/test/1.txt").writeText("123123")

        ocFile.storagePath = file.absolutePath

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")
        val folder = File("/sdcard/test/")
        folder.mkdirs()
        val file = File("/sdcard/test/1.txt")
        file.createNewFile()
        file.writeText("123123")

        ocFile.storagePath = folder.absolutePath
        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 0
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))

        ocFile.fileLength = 100
        assertFalse(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 44
        assertTrue(sut.checkIfEnoughSpace(50L, ocFile))

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(100L, ocFile))
    }

    @Test
    @SuppressWarnings("MagicNumber")
    fun testEnoughSpaceWithNoLocalFolder() {
        val sut = OCFileListFragment()
        val ocFile = OCFile("/test/")

        ocFile.mimeType = "DIR"

        ocFile.fileLength = 100
        assertTrue(sut.checkIfEnoughSpace(200L, ocFile))
    }
}
