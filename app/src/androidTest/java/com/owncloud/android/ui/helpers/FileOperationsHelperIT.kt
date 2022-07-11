/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.helpers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.MainApp
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileOperationsHelperIT {

    @Test
    fun testNull() {
        MainApp.setStoragePath(null)
        assertEquals(-1L, FileOperationsHelper.getAvailableSpaceOnDevice())
    }

    @Test
    fun testNonExistingPath() {
        MainApp.setStoragePath("/123/123")
        assertEquals(-1L, FileOperationsHelper.getAvailableSpaceOnDevice())
    }

    @Test
    fun testExistingPath() {
        MainApp.setStoragePath("/sdcard/")
        assertTrue(FileOperationsHelper.getAvailableSpaceOnDevice() > 0L)
    }
}
