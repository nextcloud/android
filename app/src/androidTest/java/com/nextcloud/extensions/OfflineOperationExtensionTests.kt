/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.utils.extensions.getParentPathFromPath
import com.nextcloud.utils.extensions.updatePath
import com.nextcloud.utils.extensions.updatePathsIfParentPathMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineOperationExtensionTests {

    /*
      private val entity = OfflineOperationEntity(
        1,
        null,
        null,
        OfflineOperationType.CreateFolder,
        "/Folder/Folder2/Folder3/",
        "Folder",
        null
    )

    @Test
    fun testGetParentPathFromPath() {
        assert(entity.getParentPathFromPath() == "/Folder/")
    }

    @Test
    fun testUpdatePath() {
        assert(entity.updatePath("/MaxPa/") == "/MaxPa/Folder2/Folder3/")
    }

    @Test
    fun testUpdatePathsIfParentPathMatches() {
        entity.path = "/MaxPa/Folder2/Folder3/"
        val oldPath = "/Folder/Folder2/Folder3/"
        assert(entity.updatePathsIfParentPathMatches(oldPath, "/MaxPa/") == "/MaxPa/Folder2/Folder3/")
    }

     */

    @Test
    fun testUpdatePathsIfParentPathMatches() {
        val entity1 = OfflineOperationEntity(path = "/abc/def/file1/")
        val entity2 = OfflineOperationEntity(path = "/xyz/file2/")

        val updatedPath1 = entity1.updatePathsIfParentPathMatches(entity1.path, "/newAbc/")
        val updatedPath2 = entity2.updatePathsIfParentPathMatches(entity2.path, "/newAbc/")

        assertEquals("/newAbc/def/file1/", updatedPath1)
        assertEquals("/newAbc/file2/", updatedPath2)
    }

    @Test
    fun testUpdatePathsIfParentPathMatchesWithNullInputs() {
        val entity = OfflineOperationEntity(path = "/abc/def/file/")

        val result1 = entity.updatePathsIfParentPathMatches(null, "/newPath/")
        val result2 = entity.updatePathsIfParentPathMatches("/oldPath/", null)

        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun testUpdatePath() {
        val entity = OfflineOperationEntity(path = "/abc/def/file/")
        val newPath = entity.updatePath("/newAbc/")
        assertEquals("/newAbc/def/file/", newPath)
    }

    @Test
    fun testUpdatePathWithNullInput() {
        val entity = OfflineOperationEntity(path = "/abc/def/file/")
        val newPath = entity.updatePath(null)
        assertNull(newPath)
    }

    @Test
    fun testGetParentPathFromPath() {
        val entity = OfflineOperationEntity(path = "/abc/def/file/")
        val parentPath = entity.getParentPathFromPath()
        assertEquals("/abc/", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithRootPath() {
        val entity = OfflineOperationEntity(path = "/")
        val parentPath = entity.getParentPathFromPath()
        assertEquals("//", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithEmptyString() {
        val entity = OfflineOperationEntity(path = "")
        val parentPath = entity.getParentPathFromPath()
        assertEquals("//", parentPath)
    }

    @Test
    fun testGetParentPathFromPathWithNullPath() {
        val entity = OfflineOperationEntity(path = null)
        val parentPath = entity.getParentPathFromPath()
        assertNull(parentPath)
    }

    @Test
    fun testUpdatePathWithEmptyPath() {
        val entity = OfflineOperationEntity(path = "")
        val newPath = entity.updatePath("/newAbc/")
        assertNull(newPath)
    }

    @Test
    fun testUpdatePathsIfParentPathMatchesWithSingleDirectoryPath() {
        val entity = OfflineOperationEntity(path = "/abc/")
        val updatedPath = entity.updatePathsIfParentPathMatches(entity.path, "/newAbc/")
        assertEquals("/newAbc/", updatedPath)
    }
}
