/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import io.mockk.EqMatcher
import io.mockk.every
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify as verifyMockito
import org.mockito.kotlin.whenever

class SynchronizeFolderTwoWaySyncTest : SynchronizeFolderOperationTestFixture() {
    @Test
    fun `ongoing sync refreshes unchanged folder listings`() {
        remoteFolder("/Root/", listOf("/Root/new.txt"))
        cacheFolder("/Root/", emptyList())

        synchronizeTree()

        verify(exactly = 1) {
            constructedWith<ReadFolderRemoteOperation>(EqMatcher("/Root/")).execute(client)
        }
        assertTrue("/Root/new.txt" in filesToSynchronize)
    }

    @Test
    fun `ongoing descendants retain local directory preparation and uploads`() {
        tree()
        cacheFolder("/Root/", listOf("/Root/A/"))
        every { storage.isPartOfInternalTwoWaySync(any()) } returns true

        synchronizeTree()

        twoWaySynchronizations.constructed().forEach {
            verifyMockito(it).ensureLocalFolderExists(any())
            verifyMockito(it).uploadNewLocalEntries(any(), any())
        }
    }

    @Test
    fun `locally deleted folders are not saved or scheduled after remote deletion`() {
        remoteFolder("/Root/", listOf("/Root/deleted/"))
        cacheFolder("/Root/", listOf("/Root/deleted/"))
        every { storage.isPartOfInternalTwoWaySync(any()) } returns true
        onTwoWaySyncCreated = {
            whenever(it.isDeletedLocally(any(), anyOrNull())).thenReturn(true)
            whenever(it.deleteRemoteFile(any(), any())).thenReturn(true)
        }

        synchronizeTree()

        verify(exactly = 1) {
            storage.saveFolder(any(), match<List<OCFile>> { it.isEmpty() }, any())
        }
        assertFalse("/Root/deleted/" in visitedFolders)
        verifyMockito(twoWaySynchronizations.constructed().single()).deleteRemoteFile(any(), any())
    }

    @Test
    fun `cancellation between downloads prevents local uploads`() {
        remoteFolder("/Root/", listOf("/Root/a.txt"))
        cacheFolder("/Root/", listOf("/Root/a.txt"))
        every { storage.isPartOfInternalTwoWaySync(any()) } returns true
        onFileSaved = { rootOperation.cancel() }

        assertFalse(executeTree().isSuccess)

        verifyMockito(twoWaySynchronizations.constructed().single(), never()).uploadNewLocalEntries(any(), any())
    }
}
