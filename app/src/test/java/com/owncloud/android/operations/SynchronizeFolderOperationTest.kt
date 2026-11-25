/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Intent
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class SynchronizeFolderOperationTest : SynchronizeFolderOperationTestFixture() {
    @Test
    fun `unchanged parent discovers previously unlisted descendants`() {
        tree()
        cacheFolder("/Root/", listOf("/Root/A/"))

        synchronizeTree()

        assertEquals(remoteChildren.keys.toList(), visitedFolders)
        assertNotNull(cachedFiles[DEEP_FILE])
        assertEquals(listOf(DEEP_FILE), filesToSynchronize)
    }

    @Test
    fun `new child folders are persisted before their synchronization starts`() {
        tree()
        cacheFolder("/Root/", emptyList())
        cachedFiles.getValue("/Root/").etag = ""

        synchronizeTree()

        assertEquals(remoteChildren.keys.toList(), visitedFolders)
        assertEquals(listOf(DEEP_FILE), filesToSynchronize)
    }

    @Test
    fun `browsing intermediate folders does not change synchronized files`() {
        tree()
        remoteChildren.forEach { (path, children) -> cacheFolder(path, children) }

        synchronizeTree()

        assertEquals(remoteChildren.keys.toList(), visitedFolders)
        assertEquals(listOf(DEEP_FILE), filesToSynchronize)
    }

    @Test
    fun `mixed files and directories are all synchronized`() {
        remoteFolder("/Root/", listOf("/Root/root.txt", "/Root/A/", "/Root/C/"))
        remoteFolder("/Root/A/", listOf("/Root/A/a.txt", "/Root/A/B/"))
        remoteFolder("/Root/A/B/", listOf("/Root/A/B/b.txt"))
        remoteFolder("/Root/C/", listOf("/Root/C/c.txt"))
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))

        synchronizeTree()

        assertEquals(
            setOf("/Root/root.txt", "/Root/A/a.txt", "/Root/A/B/b.txt", "/Root/C/c.txt"),
            filesToSynchronize.toSet()
        )
    }

    @Test
    fun `existing local files use content synchronization rather than direct download`() {
        val path = "/Root/existing.txt"
        remoteFolder("/Root/", listOf(path))
        cacheFolder("/Root/", listOf(path))
        cachedFiles.getValue(path).storagePath = localFolder.newFile().absolutePath

        synchronizeTree()

        assertEquals(listOf(path), filesToSynchronize)
        assertTrue(downloads.constructed().isEmpty())
    }

    @Test
    fun `ongoing descendant downloads finish before the root returns`() {
        tree()
        cacheFolder("/Root/", listOf("/Root/A/"))
        assertTrue(executeTree().isSuccess)
        assertEquals(listOf(DEEP_FILE), filesToSynchronize)
        verify(exactly = 0) { downloadHelper.downloadFolder(any(), any()) }
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun `a failed descendant makes the root fail while other branches continue`() {
        remoteFolder("/Root/", listOf("/Root/A/", "/Root/B/"))
        remoteFolder("/Root/A/", listOf("/Root/A/file.txt"))
        remoteFolder("/Root/B/", emptyList())
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))
        every { constructedWith<ReadFileRemoteOperation>(EqMatcher("/Root/B/")).execute(client) } returns
            RemoteOperationResult<Any>(ResultCode.FILE_NOT_FOUND)
        assertFalse(executeTree().isSuccess)
        assertEquals(listOf("/Root/A/file.txt"), filesToSynchronize)
    }

    @Test
    fun `cancellation after a child download stops remaining branches`() {
        remoteFolder("/Root/", listOf("/Root/A/", "/Root/B/"))
        remoteFolder("/Root/A/", listOf("/Root/A/file.txt"))
        remoteFolder("/Root/B/", listOf("/Root/B/file.txt"))
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))
        onFileSaved = { rootOperation.cancel() }
        assertFalse(executeTree().isSuccess)
        assertEquals(listOf("/Root/B/file.txt"), filesToSynchronize)
        assertEquals(listOf("/Root/", "/Root/B/"), visitedFolders)
    }

    @Test
    fun `a failed direct download is reported by the root`() {
        tree()
        cacheFolder("/Root/", listOf("/Root/A/"))
        downloadResult = RemoteOperationResult(ResultCode.UNKNOWN_ERROR)
        assertEquals(ResultCode.UNKNOWN_ERROR, executeTree().code)
        assertTrue(filesToSynchronize.isEmpty())
    }

    @Test
    fun `a failed local file synchronization preserves its error code`() {
        val path = "/Root/existing.txt"
        remoteFolder("/Root/", listOf(path))
        cacheFolder("/Root/", listOf(path))
        cachedFiles.getValue(path).storagePath = localFolder.newFile().absolutePath
        contentResult = RemoteOperationResult(ResultCode.UNAUTHORIZED)
        assertEquals(ResultCode.UNAUTHORIZED, executeTree().code)
    }

    @Test
    fun `a save failure does not abandon other downloads in the folder`() {
        remoteFolder("/Root/", listOf("/Root/a.txt", "/Root/b.txt"))
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))
        onFileSaved = { throw IllegalStateException("Cannot persist downloaded file") }
        assertFalse(executeTree().isSuccess)
        assertEquals(listOf("/Root/a.txt", "/Root/b.txt"), filesToSynchronize)
    }

    @Test
    fun `changed constraints stop the queue between files`() {
        remoteFolder("/Root/", listOf("/Root/a.txt", "/Root/b.txt"))
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))
        onFileSaved = { continueSync = false }
        assertFalse(executeTree().isSuccess)
        assertEquals(listOf("/Root/a.txt"), filesToSynchronize)
    }

    @Test
    fun `reusing a folder operation does not duplicate pending downloads`() {
        remoteFolder("/Root/", listOf("/Root/a.txt"))
        cacheFolder("/Root/", remoteChildren.getValue("/Root/"))
        assertTrue(executeTree().isSuccess)
        assertTrue(rootOperation.execute(client).isSuccess)
        assertEquals(listOf("/Root/a.txt", "/Root/a.txt"), filesToSynchronize)
    }

    @Test
    fun `manual folder synchronization retains service dispatch for descendants`() {
        tree()
        cacheFolder("/Root/", listOf("/Root/A/"))
        Mockito.mockConstruction(Intent::class.java).use {
            val operation = SynchronizeFolderOperation(context, "/Root/", user, storage, true, false)
            assertTrue(operation.execute(client).isSuccess)
            verify(exactly = 1) { context.startService(any()) }
            assertEquals(listOf("/Root/"), visitedFolders)
            assertTrue(filesToSynchronize.isEmpty())
        }
    }
}
