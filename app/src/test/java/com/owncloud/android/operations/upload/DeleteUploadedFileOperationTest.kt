/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import android.content.Context
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File
import java.util.concurrent.TimeUnit

class DeleteUploadedFileOperationTest {

    private val retentionMs = TimeUnit.DAYS.toMillis(RETENTION_DAYS)

    private fun operation(minAgeMs: Long) =
        DeleteUploadedFileOperation(mock<SyncedFolder>(), mock<Context>(), mock<FileDataStorageManager>(), minAgeMs)

    private fun fileModifiedDaysAgo(days: Long): File = File.createTempFile("old", null).apply {
        deleteOnExit()
        setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days))
    }

    @Test
    fun fileYoungerThanRetentionIsNotOldEnough() {
        assertFalse(operation(retentionMs).isOldEnough(fileModifiedDaysAgo(RETENTION_DAYS - 1)))
    }

    @Test
    fun fileOlderThanRetentionIsOldEnough() {
        assertTrue(operation(retentionMs).isOldEnough(fileModifiedDaysAgo(RETENTION_DAYS + 1)))
    }

    @Test
    fun noRetentionAcceptsAnyFile() {
        assertTrue(operation(SyncedFolder.AUTO_DELETE_LOCAL_DISABLED).isOldEnough(fileModifiedDaysAgo(0)))
    }

    private companion object {
        const val RETENTION_DAYS = 60L
    }
}
