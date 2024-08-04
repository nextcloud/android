/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.helpers

import com.owncloud.android.MainApp
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

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
