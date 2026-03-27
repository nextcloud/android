/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.db

import com.nextcloud.utils.extensions.sortedByUploadOrder
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus.UPLOAD_FAILED
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
import com.owncloud.android.db.OCUpload
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class OCUploadSortingTest {

    companion object {
        val failed = mock<OCUpload>(name = "failed")
        val failedLater = mock<OCUpload>(name = "failedLater")
        val failedSameTimeOtherId = mock<OCUpload>(name = "failedSameTimeOtherId")
        val equalsNotSame = mock<OCUpload>(name = "equalsNotSame")
        val inProgress = mock<OCUpload>(name = "inProgress")
        val inProgressNow = mock<OCUpload>(name = "inProgressNow")

        private const val FIXED_UPLOAD_END_TIMESTAMP = 42L
        private const val FIXED_UPLOAD_END_TIMESTAMP_LATER = 43L
        private const val UPLOAD_ID = 40L
        private const val UPLOAD_ID2 = 43L

        @JvmStatic
        @BeforeClass
        fun setupMocks() {
            MockitoAnnotations.openMocks(this)

            whenever(failed.fixedUploadStatus).thenReturn(UPLOAD_FAILED)
            whenever(inProgress.fixedUploadStatus).thenReturn(UPLOAD_IN_PROGRESS)
            whenever(inProgressNow.fixedUploadStatus).thenReturn(UPLOAD_IN_PROGRESS)
            whenever(failedLater.fixedUploadStatus).thenReturn(UPLOAD_FAILED)
            whenever(failedSameTimeOtherId.fixedUploadStatus).thenReturn(UPLOAD_FAILED)
            whenever(equalsNotSame.fixedUploadStatus).thenReturn(UPLOAD_FAILED)

            whenever(inProgressNow.isFixedUploadingNow).thenReturn(true)
            whenever(inProgress.isFixedUploadingNow).thenReturn(false)

            whenever(failed.fixedUploadEndTimeStamp).thenReturn(FIXED_UPLOAD_END_TIMESTAMP)
            whenever(failedLater.fixedUploadEndTimeStamp).thenReturn(FIXED_UPLOAD_END_TIMESTAMP_LATER)
            whenever(failedSameTimeOtherId.fixedUploadEndTimeStamp).thenReturn(FIXED_UPLOAD_END_TIMESTAMP)
            whenever(equalsNotSame.fixedUploadEndTimeStamp).thenReturn(FIXED_UPLOAD_END_TIMESTAMP)

            whenever(failedLater.fixedUploadId).thenReturn(UPLOAD_ID2)
            whenever(failedSameTimeOtherId.fixedUploadId).thenReturn(UPLOAD_ID)
            whenever(equalsNotSame.fixedUploadId).thenReturn(UPLOAD_ID)
        }
    }

    @Test
    fun `in progress comes before failed`() {
        val result = listOf(failed, inProgress).sortedByUploadOrder()
        assertEquals(listOf(inProgress, failed), result)
    }

    @Test
    fun `uploading now comes before not uploading`() {
        val result = listOf(inProgress, inProgressNow).sortedByUploadOrder()
        assertEquals(listOf(inProgressNow, inProgress), result)
    }

    @Test
    fun `later upload end comes first`() {
        val result = listOf(failed, failedLater).sortedByUploadOrder()
        assertEquals(listOf(failedLater, failed), result)
    }

    @Test
    fun `smaller upload id comes later when others equal`() {
        val result = listOf(failedLater, failedSameTimeOtherId).sortedByUploadOrder()
        assertEquals(listOf(failedLater, failedSameTimeOtherId), result)
    }

    @Test
    fun `same parameters keep stable ordering`() {
        val result = listOf(failedSameTimeOtherId, equalsNotSame).sortedByUploadOrder()
        assertEquals(listOf(failedSameTimeOtherId, equalsNotSame), result)
    }

    @Test
    fun `sort full list`() {
        val result = listOf(
            inProgress,
            inProgressNow,
            failedSameTimeOtherId,
            inProgressNow,
            failedLater,
            failed
        ).sortedByUploadOrder()

        assertEquals(
            listOf(
                inProgressNow,
                inProgressNow,
                inProgress,
                failedLater,
                failedSameTimeOtherId,
                failed
            ),
            result
        )
    }
}
