/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.fileactions

import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.logger.Logger
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.FileMenuFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

class FileActionsViewModelTest {

    private lateinit var viewModel: FileActionsViewModel

    @Before
    fun setUp() {
        viewModel = FileActionsViewModel(
            mock<CurrentAccountProvider>(),
            mock<FileMenuFilter.Factory>(),
            mock<Logger>()
        )
    }

    @Test
    fun `getLockedUntil returns null when lockTimestamp is zero`() {
        val file = OCFile("/test.docx").apply {
            lockTimestamp = 0L
            lockTimeout = 300L
        }
        assertNull(viewModel.getLockedUntil(file))
    }

    @Test
    fun `getLockedUntil returns null when lockTimeout is zero`() {
        val file = OCFile("/test.docx").apply {
            lockTimestamp = 1_700_000_000L
            lockTimeout = 0L
        }
        assertNull(viewModel.getLockedUntil(file))
    }

    @Test
    fun `getLockedUntil returns null when lockTimeout is negative (ETA_INFINITE sentinel)`() {
        val file = OCFile("/test.docx").apply {
            lockTimestamp = 1_700_000_000L
            lockTimeout = -60L
        }
        assertNull(viewModel.getLockedUntil(file))
    }

    @Test
    fun `getLockedUntil returns correct millis when both values are positive`() {
        val timestamp = 1_700_000_000L
        val timeout = 300L
        val file = OCFile("/test.docx").apply {
            lockTimestamp = timestamp
            lockTimeout = timeout
        }
        val expected = (timestamp + timeout) * TimeConstants.MILLIS_PER_SECOND
        assertEquals(expected, viewModel.getLockedUntil(file))
    }
}
