/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.content.ContentValues;

import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.utils.FileStorageUtils;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.GALLERY_SEARCH;
import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.PHOTO_SEARCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

abstract public class FileDataStorageManagerIT extends AbstractOnServerIT {

    protected FileDataStorageManager sut;

    @Before
    public void before() {
        // make sure everything is removed
        sut.deleteAllFiles();
        sut.deleteVirtuals(VirtualFolderType.GALLERY);

        assertEquals(0, sut.getAllFiles().size());
    }

    @After
    public void after() {
        super.after();

        sut.deleteAllFiles();
        sut.deleteVirtuals(VirtualFolderType.GALLERY);
    }

    @Test
    public void simpleTest() {
        OCFile file = sut.getFileByDecryptedRemotePath("/");
        assertNotNull(file);
        assertTrue(file.fileExists());
        assertNull(sut.getFileByDecryptedRemotePath("/123123"));
    }

    @Test
    public void getAllFiles_NoAvailable() {
        assertEquals(0, sut.getAllFiles().size());
    }

    @Test
    public void testFolderContent() throws IOException {
        assertEquals(0, sut.getAllFiles().size());
        assertTrue(new CreateFolderRemoteOperation("/1/1/", true).execute(client).isSuccess());

        assertTrue(new CreateFolderRemoteOperation("/1/2/", true).execute(client).isSuccess());

        assertTrue(new UploadFileRemoteOperation(getDummyFile("/chunkedFile.txt").getAbsolutePath(),
                                                 "/1/1/chunkedFile.txt",
                                                 "text/plain",
                                                 String.valueOf(System.currentTimeMillis() / 1000))
                       .execute(client).isSuccess());

        assertTrue(new UploadFileRemoteOperation(getDummyFile("/chunkedFile.txt").getAbsolutePath(),
                                                 "/1/1/chunkedFile2.txt",
                                                 "text/plain",
                                                 String.valueOf(System.currentTimeMillis() / 1000))
                       .execute(client).isSuccess());

        File imageFile = getFile("imageFile.png");
        assertTrue(new UploadFileRemoteOperation(imageFile.getAbsolutePath(),
                                                 "/1/1/imageFile.png",
                                                 "image/png",
                                                 String.valueOf(System.currentTimeMillis() / 1000))
                       .execute(client).isSuccess());

        // sync
        assertNull(sut.getFileByDecryptedRemotePath("/1/1/"));

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/1/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/1/1/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        assertEquals(3, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/1/1/"), false).size());
    }

    /**
     * This test creates an image, does a photo search (now returned image is not yet in file hierarchy), then root
     * folder is refreshed and it is verified that the same image file is used in database
     */
    @Test
    public void testPhotoSearch() throws IOException {
        String remotePath = "/imageFile.png";
        VirtualFolderType virtualType = VirtualFolderType.GALLERY;

        assertEquals(0, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());
        assertEquals(1, sut.getAllFiles().size());

        File imageFile = getFile("imageFile.png");
        assertTrue(new UploadFileRemoteOperation(imageFile.getAbsolutePath(),
                                                 remotePath,
                                                 "image/png",
                                                 String.valueOf(System.currentTimeMillis() / 1000))
                       .execute(client).isSuccess());

        assertNull(sut.getFileByDecryptedRemotePath(remotePath));

        // search
        SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation("image/%",
                                                                                PHOTO_SEARCH,
                                                                                false);

        RemoteOperationResult<List<RemoteFile>> searchResult = searchRemoteOperation.execute(client);
        TestCase.assertTrue(searchResult.isSuccess());
        TestCase.assertEquals(1, searchResult.getResultData().size());

        OCFile ocFile = FileStorageUtils.fillOCFile(searchResult.getResultData().get(0));
        sut.saveFile(ocFile);

        List<ContentValues> contentValues = new ArrayList<>();
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, virtualType.toString());
        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

        contentValues.add(cv);

        sut.saveVirtuals(contentValues);

        assertEquals(remotePath, ocFile.getRemotePath());

        assertEquals(0, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());

        assertEquals(1, sut.getVirtualFolderContent(virtualType, false).size());
        assertEquals(2, sut.getAllFiles().size());

        // update root
        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());


        assertEquals(1, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());
        assertEquals(1, sut.getVirtualFolderContent(virtualType, false).size());
        assertEquals(2, sut.getAllFiles().size());

        assertEquals(sut.getVirtualFolderContent(virtualType, false).get(0),
                     sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).get(0));
    }

    /**
     * This test creates an image and a video, does a gallery search (now returned image and video is not yet in file
     * hierarchy), then root folder is refreshed and it is verified that the same image file is used in database
     */
    @Test
    public void testGallerySearch() throws IOException {
        sut = new FileDataStorageManager(user,
                                         targetContext
                                             .getContentResolver()
                                             .acquireContentProviderClient(ProviderMeta.ProviderTableMeta.CONTENT_URI)
        );

        String imagePath = "/imageFile.png";
        VirtualFolderType virtualType = VirtualFolderType.GALLERY;

        assertEquals(0, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());
        assertEquals(1, sut.getAllFiles().size());

        File imageFile = getFile("imageFile.png");
        assertTrue(new UploadFileRemoteOperation(imageFile.getAbsolutePath(),
                                                 imagePath,
                                                 "image/png",
                                                 String.valueOf((System.currentTimeMillis() - 10000) / 1000))
                       .execute(client).isSuccess());

        // Check that file does not yet exist in local database
        assertNull(sut.getFileByDecryptedRemotePath(imagePath));

        String videoPath = "/videoFile.mp4";
        File videoFile = getFile("videoFile.mp4");
        assertTrue(new UploadFileRemoteOperation(videoFile.getAbsolutePath(),
                                                 videoPath,
                                                 "video/mpeg",
                                                 String.valueOf((System.currentTimeMillis() + 10000) / 1000))
                       .execute(client).isSuccess());

        // Check that file does not yet exist in local database
        assertNull(sut.getFileByDecryptedRemotePath(videoPath));

        // search
        SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation("",
                                                                                GALLERY_SEARCH,
                                                                                false);

        RemoteOperationResult<List<RemoteFile>> searchResult = searchRemoteOperation.execute(client);
        TestCase.assertTrue(searchResult.isSuccess());
        TestCase.assertEquals(2, searchResult.getResultData().size());

        // newest file must be video path (as sorted by recently modified)
        OCFile ocFile = FileStorageUtils.fillOCFile( searchResult.getResultData().get(0));
        sut.saveFile(ocFile);
        assertEquals(videoPath, ocFile.getRemotePath());

        List<ContentValues> contentValues = new ArrayList<>();
        ContentValues cv = new ContentValues();
        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, virtualType.toString());
        cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

        contentValues.add(cv);

        // second is image file, as older
        OCFile ocFile2 = FileStorageUtils.fillOCFile(searchResult.getResultData().get(1));
        sut.saveFile(ocFile2);
        assertEquals(imagePath, ocFile2.getRemotePath());

        ContentValues cv2 = new ContentValues();
        cv2.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, virtualType.toString());
        cv2.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile2.getFileId());

        contentValues.add(cv2);

        sut.saveVirtuals(contentValues);

        assertEquals(0, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());

        assertEquals(2, sut.getVirtualFolderContent(virtualType, false).size());
        assertEquals(3, sut.getAllFiles().size());

        // update root
        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());


        assertEquals(2, sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).size());
        assertEquals(2, sut.getVirtualFolderContent(virtualType, false).size());
        assertEquals(3, sut.getAllFiles().size());

        assertEquals(sut.getVirtualFolderContent(virtualType, false).get(0),
                     sut.getFolderContent(sut.getFileByDecryptedRemotePath("/"), false).get(0));
    }

    @Test
    public void testSaveNewFile() {
        assertTrue(new CreateFolderRemoteOperation("/1/1/", true).execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/1/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        assertTrue(new RefreshFolderOperation(sut.getFileByDecryptedRemotePath("/1/1/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              sut,
                                              user,
                                              targetContext).execute(client).isSuccess());

        OCFile newFile = new OCFile("/1/1/1.txt");
        newFile.setRemoteId("123");

        sut.saveNewFile(newFile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSaveNewFile_NonExistingParent() {
        assertTrue(new CreateFolderRemoteOperation("/1/1/", true).execute(client).isSuccess());

        OCFile newFile = new OCFile("/1/1/1.txt");

        sut.saveNewFile(newFile);
    }

    @Test
    public void testOCCapability() {
        OCCapability capability = new OCCapability();
        capability.setUserStatus(CapabilityBooleanType.TRUE);

        sut.saveCapabilities(capability);

        OCCapability newCapability = sut.getCapability(user);

        assertEquals(capability.getUserStatus(), newCapability.getUserStatus());
    }
}
