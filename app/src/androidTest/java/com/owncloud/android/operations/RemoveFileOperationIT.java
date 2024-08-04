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

import static junit.framework.TestCase.assertNotNull;
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
