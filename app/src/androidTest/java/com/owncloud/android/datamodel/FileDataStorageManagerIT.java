/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.GALLERY_SEARCH;
import static com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType.PHOTO_SEARCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

abstract public class FileDataStorageManagerIT extends AbstractOnServerIT {

    protected FileDataStorageManager sut;
    private OCCapability capability;

    @Before
    public void before() {
        // make sure everything is removed
        sut.deleteAllFiles();
        sut.deleteVirtuals(VirtualFolderType.GALLERY);

        assertEquals(0, sut.getAllFiles().size());

        capability = (OCCapability) new GetCapabilitiesRemoteOperation(null)
            .execute(client)
            .getSingleData();
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

        assertTrue(new UploadFileRemoteOperation(getDummyFile("chunkedFile.txt").getAbsolutePath(),
                                                 "/1/1/chunkedFile.txt",
                                                 "text/plain",
                                                 System.currentTimeMillis() / 1000)
                       .execute(client).isSuccess());

        assertTrue(new UploadFileRemoteOperation(getDummyFile("chunkedFile.txt").getAbsolutePath(),
                                                 "/1/1/chunkedFile2.txt",
                                                 "text/plain",
                                                 System.currentTimeMillis() / 1000)
                       .execute(client).isSuccess());

        File imageFile = getFile("imageFile.png");
        assertTrue(new UploadFileRemoteOperation(imageFile.getAbsolutePath(),
                                                 "/1/1/imageFile.png",
                                                 "image/png",
                                                 System.currentTimeMillis() / 1000)
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

    @Test
    public void testQuery() {
        assertEquals(0, sut.getAllFiles().size());
        OCFile root = new OCFile("/");
        root.setRemoteId("00000001");
        root.setMimeType(MimeType.WEBDAV_FOLDER);
        root.setOwnerId(getUserId(user));
        sut.saveFile(root);
        root = sut.getFileByEncryptedRemotePath("/");

        OCFile file1 = new OCFile("/1/");
        file1.setRemoteId("00000002");
        file1.setMimeType(MimeType.WEBDAV_FOLDER);
        file1.setParentId(root.getFileId());
        file1.setOwnerId(getUserId(user));
        file1.setCreationTimestamp(50L);
        file1.setFileLength(100L);
        sut.saveFile(file1);

        OCFile file2 = new OCFile("/2/");
        file2.setRemoteId("00000003");
        file2.setMimeType(MimeType.WEBDAV_FOLDER);
        file2.setParentId(root.getFileId());
        file2.setOwnerId(getUserId(user));
        file2.setFavorite(true);
        file2.setCreationTimestamp(30L);
        file2.setFileLength(2L);
        sut.saveFile(file2);

        OCFile file3 = new OCFile("/.hidden/");
        file3.setRemoteId("00000004");
        file3.setMimeType(MimeType.WEBDAV_FOLDER);
        file3.setParentId(root.getFileId());
        file3.setOwnerId(getUserId(user));
        file3.setCreationTimestamp(90L);
        file3.setFileLength(99L);
        sut.saveFile(file3);

        OCFile file4 = new OCFile("/test.png");
        file4.setRemoteId("00000005");
        file4.setMimeType(MimeType.PNG);
        file4.setParentId(root.getFileId());
        file4.setCreationTimestamp(22L);
        file4.setFileLength(12L);
        sut.saveFile(file4);

        OCFile file5 = new OCFile("/test.jpg");
        file5.setRemoteId("00000006");
        file5.setMimeType(MimeType.JPEG);
        file5.setParentId(root.getFileId());
        file5.setOwnerId(getUserId(user));
        file5.setCreationTimestamp(2L);
        file5.setFileLength(55L);
        sut.saveFile(file5);

        OCFile file6 = new OCFile("/sharedWithMe/");
        file6.setRemoteId("00000006");
        file6.setMimeType(MimeType.WEBDAV_FOLDER);
        file6.setParentId(root.getFileId());
        file6.setPermissions("S");
        file6.setOwnerId(getUserId(user));
        file6.setCreationTimestamp(100L);
        file6.setFileLength(111L);
        sut.saveFile(file6);

        OCFile file7 = new OCFile("/groupfolder/");
        file7.setRemoteId("00000006");
        file7.setMimeType(MimeType.WEBDAV_FOLDER);
        file7.setParentId(root.getFileId());
        file7.setPermissions("M");
        file7.setOwnerId(getUserId(user));
        file7.setCreationTimestamp(120L);
        file7.setFileLength(90L);
        sut.saveFile(file7);

        assertEquals(8, sut.getAllFiles().size());

        // all files
        assertEquals(7, sut.getFolderContent(root,
                                             false,
                                             true,
                                             "",
                                             false,
                                             FileSortOrder.SORT_A_TO_Z).size());

        // no hidden files
        assertEquals(6, sut.getFolderContent(root,
                                             false,
                                             false,
                                             "",
                                             false,
                                             FileSortOrder.SORT_A_TO_Z).size());

        // mimetype - PNG
        assertEquals(1, sut.getFolderContent(root,
                                             false,
                                             false,
                                             MimeType.PNG,
                                             false,
                                             FileSortOrder.SORT_A_TO_Z).size());

        // mimetype - all images
        assertEquals(2, sut.getFolderContent(root,
                                             false,
                                             false,
                                             MimeType.IMAGES,
                                             false,
                                             FileSortOrder.SORT_A_TO_Z).size());

        // personal files
        assertEquals(4, sut.getFolderContent(root,
                                             false,
                                             true,
                                             "",
                                             true,
                                             FileSortOrder.SORT_A_TO_Z).size());

        // sort A-Z
        List<OCFile> sortedA_Z = Arrays.asList(
            file2,
            file3,
            file1,
            file7,
            file6,
            file5,
            file4
                                              );
        List<OCFile> actualA_Z = sut.getFolderContent(root,
                                                      false,
                                                      true,
                                                      "",
                                                      false,
                                                      FileSortOrder.SORT_A_TO_Z);
        assertTrue(testSorting(sortedA_Z, actualA_Z));

        // sort Z-A
        List<OCFile> sortedZ_A = Arrays.asList(
            file2,
            file4,
            file5,
            file6,
            file7,
            file1,
            file3
                                              );
        List<OCFile> actualZ_A = sut.getFolderContent(root,
                                                      false,
                                                      true,
                                                      "",
                                                      false,
                                                      FileSortOrder.SORT_Z_TO_A);
        assertTrue(testSorting(sortedZ_A, actualZ_A));

        // sort old-new
        List<OCFile> sortedOld_New = Arrays.asList(
            file2,
            file5,
            file4,
            file1,
            file3,
            file6,
            file7
                                                  );
        List<OCFile> actualOld_New = sut.getFolderContent(root,
                                                          false,
                                                          true,
                                                          "",
                                                          false,
                                                          FileSortOrder.SORT_OLD_TO_NEW);
        assertTrue(testSorting(sortedOld_New, actualOld_New));

        // sort new-old
        List<OCFile> sortedNew_Old = Arrays.asList(
            file2,
            file7,
            file6,
            file3,
            file1,
            file4,
            file5
                                                  );
        List<OCFile> actualNew_Old = sut.getFolderContent(root,
                                                          false,
                                                          true,
                                                          "",
                                                          false,
                                                          FileSortOrder.SORT_NEW_TO_OLD);
        assertTrue(testSorting(sortedNew_Old, actualNew_Old));

        // sort small-big
        List<OCFile> sortedSmall_Big = Arrays.asList(
            file2,
            file4, // 22
            file5, // 55
            file7, // 90
            file3, // 99
            file1, // 100
            file6 // 111
                                                    );
        List<OCFile> actualSmall_Big = sut.getFolderContent(root,
                                                            false,
                                                            true,
                                                            "",
                                                            false,
                                                            FileSortOrder.SORT_SMALL_TO_BIG);
        assertTrue(testSorting(sortedSmall_Big, actualSmall_Big));

        // sort big-small
        List<OCFile> sortedBig_Small = Arrays.asList(
            file2,
            file6, // 111
            file1, // 100
            file3, // 99
            file7, // 90
            file5, // 55
            file4 // 22
                                                    );
        List<OCFile> actualBig_Small = sut.getFolderContent(root,
                                                            false,
                                                            true,
                                                            "",
                                                            false,
                                                            FileSortOrder.SORT_BIG_TO_SMALL);
        assertTrue(testSorting(sortedBig_Small, actualBig_Small));
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
                                                 System.currentTimeMillis() / 1000)
                       .execute(client).isSuccess());

        assertNull(sut.getFileByDecryptedRemotePath(remotePath));

        // search
        SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation("image/%",
                                                                                PHOTO_SEARCH,
                                                                                false,
                                                                                capability);

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
                                                 (System.currentTimeMillis() - 10000) / 1000)
                       .execute(client).isSuccess());

        // Check that file does not yet exist in local database
        assertNull(sut.getFileByDecryptedRemotePath(imagePath));

        String videoPath = "/videoFile.mp4";
        File videoFile = getFile("videoFile.mp4");
        assertTrue(new UploadFileRemoteOperation(videoFile.getAbsolutePath(),
                                                 videoPath,
                                                 "video/mpeg",
                                                 (System.currentTimeMillis() + 10000) / 1000)
                       .execute(client).isSuccess());

        // Check that file does not yet exist in local database
        assertNull(sut.getFileByDecryptedRemotePath(videoPath));

        // search
        SearchRemoteOperation searchRemoteOperation = new SearchRemoteOperation("",
                                                                                GALLERY_SEARCH,
                                                                                false,
                                                                                capability);

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
        newFile.setRemoteId("12345678");

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

    private boolean testSorting(List<OCFile> target, List<OCFile> actual) {

        for (int i = 0; i < target.size(); i++) {
            boolean compare;

            compare = target.get(i).getRemoteId().equals(actual.get(i).getRemoteId());

            if (!compare) {

                System.out.println("target:");

                for (OCFile item : target) {
                    if (item == null) {
                        System.out.println("null");
                    } else {
                        System.out.println(item.getFileName());
                    }
                }

                System.out.println();
                System.out.println("actual:");
                for (OCFile item : actual) {
                    if (item == null) {
                        System.out.println("null");
                    } else {
                        System.out.println(item.getFileName());
                    }
                }

                return false;
            }
        }

        return true;
    }
}
