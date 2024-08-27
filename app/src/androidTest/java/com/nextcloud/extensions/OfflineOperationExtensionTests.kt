/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.utils.extensions.getTopParentPathFromPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineOperationExtensionTests {

    @Test
    fun testGetParentPathFromPath() {
        val entity = OfflineOperationEntity(path = "/abc/def/file/")
        val parentPath = entity.getTopParentPathFromPath()
        assertEquals("/abc/", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithRootPath() {
        val entity = OfflineOperationEntity(path = "/")
        val parentPath = entity.getTopParentPathFromPath()
        assertEquals("//", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithEmptyString() {
        val entity = OfflineOperationEntity(path = "")
        val parentPath = entity.getTopParentPathFromPath()
        assertEquals("//", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithNullPath() {
        val entity = OfflineOperationEntity(path = null)
        val parentPath = entity.getTopParentPathFromPath()
        assertNull(parentPath)
    }
}
