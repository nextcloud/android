/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

public class RemoveFileOperationIT extends AbstractOnServerIT {
    @Test
    public void deleteFolder() {
        String parent = "/test/";
        String path = parent + "folder1/";
        assertTrue(new CreateFolderOperation(path, user, targetContext, getStorageManager()).execute(client)
                       .isSuccess());

        OCFile folder = getStorageManager().getFileByPath(path);

        assertNotNull(folder);

        assertTrue(new RemoveFileOperation(folder,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());

        OCFile parentFolder = getStorageManager().getFileByPath(parent);

        assertNotNull(parentFolder);
        assertTrue(new RemoveFileOperation(parentFolder,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());

        assertNull(getStorageManager().getFileByPath(parent));
        assertNull(getStorageManager().getFileByPath(path));
        assertNotNull(getStorageManager().getFileByPath("/"));
    }

    @Test
    public void deleteOnlyParentFolder() {
        // create some more data
        createFolder("/test1/");
        createFolder("/test2/");
        
        String parent = "/test/";
        String path = parent + "folder1/";

        OCFile parentFolder = getStorageManager().getFileByPath(parent);
        assertNull(parentFolder);

        OCFile folder = getStorageManager().getFileByPath(path);
        assertNull(folder);

        createFolder(path);
        folder = getStorageManager().getFileByPath(path);
        assertNotNull(folder);

        parentFolder = getStorageManager().getFileByPath(parent);
        assertNotNull(parentFolder);
        
        assertEquals(5, getStorageManager().getAllFiles().size());

        assertTrue(new RemoveFileOperation(parentFolder,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());

        assertNull(getStorageManager().getFileByPath(parent));
        assertNull(getStorageManager().getFileByPath(path));
        assertNotNull(getStorageManager().getFileByPath("/"));

        assertEquals(3, getStorageManager().getAllFiles().size());
    }

    @Test
    public void deleteFile() throws IOException {
        String parent = "/test/";
        String path = parent + "empty.txt";
        OCUpload ocUpload = new OCUpload(getDummyFile("empty.txt").getAbsolutePath(), path, account.name);

        uploadOCUpload(ocUpload);

        OCFile file = getStorageManager().getFileByPath(path);

        assertNotNull(file);

        assertTrue(new RemoveFileOperation(file,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());

        OCFile parentFolder = getStorageManager().getFileByPath(parent);

        assertNotNull(parentFolder);
        assertTrue(new RemoveFileOperation(parentFolder,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());
    }
}
