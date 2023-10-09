/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel

import android.os.Parcel
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit test, to be run in an Android emulator or device.
 *
 * At the moment, it's a sample to validate the automatic test environment, in the scope of instrumented unit tests.
 * vvlcvlc
 * Don't take it as an example of completeness.
 *
 * See http://developer.android.com/intl/es/training/testing/unit-testing/instrumented-unit-tests.html .
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class OCFileUnitTest {
    private var mFile: OCFile? = null
    @Before
    fun createDefaultOCFile() {
        mFile = OCFile(PATH)
    }

    @Test
    fun writeThenReadAsParcelable() {

        // Set up mFile with not-default values
        mFile!!.fileId = ID
        mFile!!.parentId = PARENT_ID
        mFile!!.storagePath = STORAGE_PATH
        mFile!!.mimeType = MIME_TYPE
        mFile!!.fileLength = FILE_LENGTH
        mFile!!.creationTimestamp = CREATION_TIMESTAMP
        mFile!!.modificationTimestamp = MODIFICATION_TIMESTAMP
        mFile!!.modificationTimestampAtLastSyncForData =
            MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA
        mFile!!.lastSyncDateForProperties = LAST_SYNC_DATE_FOR_PROPERTIES
        mFile!!.lastSyncDateForData = LAST_SYNC_DATE_FOR_DATA
        mFile!!.etag = ETAG
        mFile!!.isSharedViaLink = true
        mFile!!.isSharedWithSharee = true
        mFile!!.permissions = PERMISSIONS
        mFile!!.remoteId = REMOTE_ID
        mFile!!.isUpdateThumbnailNeeded = true
        mFile!!.isDownloading = true
        mFile!!.etagInConflict = ETAG_IN_CONFLICT

        // Write the file data in a Parcel
        val parcel = Parcel.obtain()
        mFile!!.writeToParcel(parcel, mFile!!.describeContents())

        // Read the data from the parcel
        parcel.setDataPosition(0)
        val fileReadFromParcel = OCFile.CREATOR.createFromParcel(parcel)

        // Verify that the received data are correct
        MatcherAssert.assertThat(fileReadFromParcel.remotePath, CoreMatchers.`is`(PATH))
        MatcherAssert.assertThat(fileReadFromParcel.fileId, CoreMatchers.`is`(ID))
        MatcherAssert.assertThat(fileReadFromParcel.parentId, CoreMatchers.`is`(PARENT_ID))
        MatcherAssert.assertThat(fileReadFromParcel.storagePath, CoreMatchers.`is`(STORAGE_PATH))
        MatcherAssert.assertThat(fileReadFromParcel.mimeType, CoreMatchers.`is`(MIME_TYPE))
        MatcherAssert.assertThat(fileReadFromParcel.fileLength, CoreMatchers.`is`(FILE_LENGTH))
        MatcherAssert.assertThat(fileReadFromParcel.creationTimestamp, CoreMatchers.`is`(CREATION_TIMESTAMP))
        MatcherAssert.assertThat(fileReadFromParcel.modificationTimestamp, CoreMatchers.`is`(MODIFICATION_TIMESTAMP))
        MatcherAssert.assertThat(
            fileReadFromParcel.modificationTimestampAtLastSyncForData,
            CoreMatchers.`is`(MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA)
        )
        MatcherAssert.assertThat(
            fileReadFromParcel.lastSyncDateForProperties, CoreMatchers.`is`(
                LAST_SYNC_DATE_FOR_PROPERTIES
            )
        )
        MatcherAssert.assertThat(fileReadFromParcel.lastSyncDateForData, CoreMatchers.`is`(LAST_SYNC_DATE_FOR_DATA))
        MatcherAssert.assertThat(fileReadFromParcel.etag, CoreMatchers.`is`(ETAG))
        MatcherAssert.assertThat(fileReadFromParcel.isSharedViaLink, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(fileReadFromParcel.isSharedWithSharee, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(fileReadFromParcel.permissions, CoreMatchers.`is`(PERMISSIONS))
        MatcherAssert.assertThat(fileReadFromParcel.remoteId, CoreMatchers.`is`(REMOTE_ID))
        MatcherAssert.assertThat(fileReadFromParcel.isUpdateThumbnailNeeded, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(fileReadFromParcel.isDownloading, CoreMatchers.`is`(true))
        MatcherAssert.assertThat(fileReadFromParcel.etagInConflict, CoreMatchers.`is`(ETAG_IN_CONFLICT))
    }

    companion object {
        private const val PATH = "/path/to/a/file.txt"
        private const val ID = 12345L
        private const val PARENT_ID = 567890L
        private const val STORAGE_PATH = "/mnt/sd/localpath/to/a/file.txt"
        private const val MIME_TYPE = "text/plain"
        private const val FILE_LENGTH = 9876543210L
        private const val CREATION_TIMESTAMP = 8765432109L
        private const val MODIFICATION_TIMESTAMP = 7654321098L
        private const val MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA = 6543210987L
        private const val LAST_SYNC_DATE_FOR_PROPERTIES = 5432109876L
        private const val LAST_SYNC_DATE_FOR_DATA = 4321098765L
        private const val ETAG = "adshfas98ferqw8f9yu2"
        private const val PUBLIC_LINK = "https://nextcloud.localhost/owncloud/987427448712984sdas29"
        private const val PERMISSIONS = "SRKNVD"
        private const val REMOTE_ID = "jadñgiadf8203:9jrp98v2mn3er2089fh"
        private const val ETAG_IN_CONFLICT = "2adshfas98ferqw8f9yu"
    }
}