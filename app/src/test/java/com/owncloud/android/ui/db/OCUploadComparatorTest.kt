/*
 * NextCloud Android client application
 *
 * @copyright Copyright (C) 2019 Daniele Fognini <dfogni@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.db

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus.UPLOAD_FAILED
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.OCUploadComparator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
    OCUploadComparatorTest.Ordering::class,
    OCUploadComparatorTest.ComparatorContract::class
)
class OCUploadComparatorTest {

    internal abstract class Base {
        companion object {
            val failed = mock<OCUpload>(name = "failed")
            val failedLater = mock<OCUpload>(name = "failedLater")
            val failedSameTimeOtherId = mock<OCUpload>(name = "failedSameTimeOtherId")
            val equalsNotSame = mock<OCUpload>(name = "equalsNotSame")
            val inProgress = mock<OCUpload>(name = "InProgress")
            val inProgressNow = mock<OCUpload>(name = "inProgressNow")
            private const val FIXED_UPLOAD_END_TIMESTAMP = 42L
            private const val FIXED_UPLOAD_END_TIMESTAMP_LATER = 43L
            private const val UPLOAD_ID = 40L
            private const val UPLOAD_ID2 = 43L

            fun uploads(): Array<OCUpload> {
                return arrayOf(failed, failedLater, inProgress, inProgressNow, failedSameTimeOtherId)
            }

            @JvmStatic
            @BeforeClass
            fun setupMocks() {
                MockitoAnnotations.initMocks(this)

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

                whenever(failedLater.uploadId).thenReturn(UPLOAD_ID2)
                whenever(failedSameTimeOtherId.uploadId).thenReturn(UPLOAD_ID)
                whenever(equalsNotSame.uploadId).thenReturn(UPLOAD_ID)
            }
        }
    }

    internal class Ordering : Base() {

        @Test
        fun `same are compared equals in the list`() {
            assertEquals(0, OCUploadComparator().compare(failed, failed))
        }

        @Test
        fun `in progress is before failed in the list`() {
            assertEquals(1, OCUploadComparator().compare(failed, inProgress))
        }

        @Test
        fun `in progress uploading now is before in progress in the list`() {
            assertEquals(1, OCUploadComparator().compare(inProgress, inProgressNow))
        }

        @Test
        fun `later upload end is earlier in the list`() {
            assertEquals(1, OCUploadComparator().compare(failed, failedLater))
        }

        @Test
        fun `smaller upload id is earlier in the list`() {
            assertEquals(1, OCUploadComparator().compare(failed, failedLater))
        }

        @Test
        fun `same parameters compare equal in the list`() {
            assertEquals(0, OCUploadComparator().compare(failedSameTimeOtherId, equalsNotSame))
        }

        @Test
        fun `sort some uploads in the list`() {
            val array = arrayOf(
                inProgress,
                inProgressNow,
                failedSameTimeOtherId,
                inProgressNow,
                null,
                failedLater,
                failed
            )

            array.sortWith(OCUploadComparator())

            assertArrayEquals(
                arrayOf(
                    null,
                    inProgressNow,
                    inProgressNow,
                    inProgress,
                    failedLater,
                    failedSameTimeOtherId,
                    failed
                ),
                array
            )
        }
    }

    @RunWith(Parameterized::class)
    internal class ComparatorContract(
        private val upload1: OCUpload,
        private val upload2: OCUpload,
        private val upload3: OCUpload
    ) : Base() {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}, {1}, {2}")
            fun data(): List<Array<OCUpload>> {
                return uploads().flatMap { u1 ->
                    uploads().flatMap { u2 ->
                        uploads().map { u3 ->
                            arrayOf(u1, u2, u3)
                        }
                    }
                }
            }
        }

        @Test
        fun `comparator is reflective`() {
            assertEquals(
                -OCUploadComparator().compare(upload1, upload2),
                OCUploadComparator().compare(upload2, upload1)
            )
        }

        @Test
        fun `comparator is compatible with equals`() {
            if (upload1 == upload2) {
                assertEquals(0, OCUploadComparator().compare(upload1, upload2))
            }
        }

        @Test
        fun `comparator is transitive`() {
            val compare12 = OCUploadComparator().compare(upload1, upload2)
            val compare23 = OCUploadComparator().compare(upload2, upload3)

            if (compare12 == compare23) {
                assertEquals(compare12, OCUploadComparator().compare(upload1, upload3))
            }
        }
    }
}
