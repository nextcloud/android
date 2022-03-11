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


package com.owncloud.android.datamodel;

import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Instrumented unit test, to be run in an Android emulator or device.
 *
 * At the moment, it's a sample to validate the automatic test environment, in the scope of instrumented unit tests.
 *vvlcvlc
 * Don't take it as an example of completeness.
 *
 * See http://developer.android.com/intl/es/training/testing/unit-testing/instrumented-unit-tests.html .
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class OCFileUnitTest {

    private final static String PATH = "/path/to/a/file.txt";
    private static final long ID = 12345L;
    private static final long PARENT_ID = 567890L;
    private static final String STORAGE_PATH = "/mnt/sd/localpath/to/a/file.txt";
    private static final String MIME_TYPE = "text/plain";
    private static final long FILE_LENGTH = 9876543210L;
    private static final long CREATION_TIMESTAMP = 8765432109L;
    private static final long MODIFICATION_TIMESTAMP = 7654321098L;
    private static final long MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA = 6543210987L;
    private static final long LAST_SYNC_DATE_FOR_PROPERTIES = 5432109876L;
    private static final long LAST_SYNC_DATE_FOR_DATA = 4321098765L;
    private static final String ETAG = "adshfas98ferqw8f9yu2";
    private static final String PUBLIC_LINK = "https://nextcloud.localhost/owncloud/987427448712984sdas29";
    private static final String PERMISSIONS = "SRKNVD";
    private static final String REMOTE_ID = "jadñgiadf8203:9jrp98v2mn3er2089fh";
    private static final String ETAG_IN_CONFLICT = "2adshfas98ferqw8f9yu";

    private OCFile mFile;

    @Before
    public void createDefaultOCFile() {
        mFile = new OCFile(PATH);
    }


    @Test
    public void writeThenReadAsParcelable() {

        // Set up mFile with not-default values
        mFile.setFileId(ID);
        mFile.setParentId(PARENT_ID);
        mFile.setStoragePath(STORAGE_PATH);
        mFile.setMimeType(MIME_TYPE);
        mFile.setFileLength(FILE_LENGTH);
        mFile.setCreationTimestamp(CREATION_TIMESTAMP);
        mFile.setModificationTimestamp(MODIFICATION_TIMESTAMP);
        mFile.setModificationTimestampAtLastSyncForData(MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA);
        mFile.setLastSyncDateForProperties(LAST_SYNC_DATE_FOR_PROPERTIES);
        mFile.setLastSyncDateForData(LAST_SYNC_DATE_FOR_DATA);
        mFile.setEtag(ETAG);
        mFile.setSharedViaLink(true);
        mFile.setSharedWithSharee(true);
        mFile.setPermissions(PERMISSIONS);
        mFile.setRemoteId(REMOTE_ID);
        mFile.setUpdateThumbnailNeeded(true);
        mFile.setDownloading(true);
        mFile.setEtagInConflict(ETAG_IN_CONFLICT);


        // Write the file data in a Parcel
        Parcel parcel = Parcel.obtain();
        mFile.writeToParcel(parcel, mFile.describeContents());

        // Read the data from the parcel
        parcel.setDataPosition(0);
        OCFile fileReadFromParcel = OCFile.CREATOR.createFromParcel(parcel);

        // Verify that the received data are correct
        assertThat(fileReadFromParcel.getRemotePath(), is(PATH));
        assertThat(fileReadFromParcel.getFileId(), is(ID));
        assertThat(fileReadFromParcel.getParentId(), is(PARENT_ID));
        assertThat(fileReadFromParcel.getStoragePath(), is(STORAGE_PATH));
        assertThat(fileReadFromParcel.getMimeType(), is(MIME_TYPE));
        assertThat(fileReadFromParcel.getFileLength(), is(FILE_LENGTH));
        assertThat(fileReadFromParcel.getCreationTimestamp(), is(CREATION_TIMESTAMP));
        assertThat(fileReadFromParcel.getModificationTimestamp(), is(MODIFICATION_TIMESTAMP));
        assertThat(
                fileReadFromParcel.getModificationTimestampAtLastSyncForData(),
                is(MODIFICATION_TIMESTAMP_AT_LAST_SYNC_FOR_DATA)
        );
        assertThat(fileReadFromParcel.getLastSyncDateForProperties(), is(LAST_SYNC_DATE_FOR_PROPERTIES));
        assertThat(fileReadFromParcel.getLastSyncDateForData(), is(LAST_SYNC_DATE_FOR_DATA));
        assertThat(fileReadFromParcel.getEtag(), is(ETAG));
        assertThat(fileReadFromParcel.isSharedViaLink(), is(true));
        assertThat(fileReadFromParcel.isSharedWithSharee(), is(true));
        assertThat(fileReadFromParcel.getPermissions(), is(PERMISSIONS));
        assertThat(fileReadFromParcel.getRemoteId(), is(REMOTE_ID));
        assertThat(fileReadFromParcel.isUpdateThumbnailNeeded(), is(true));
        assertThat(fileReadFromParcel.isDownloading(), is(true));
        assertThat(fileReadFromParcel.getEtagInConflict(), is(ETAG_IN_CONFLICT));
    }
}
