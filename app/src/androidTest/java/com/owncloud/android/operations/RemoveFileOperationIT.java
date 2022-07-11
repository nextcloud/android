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

package com.owncloud.android.operations;

import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
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
        OCUpload ocUpload = new OCUpload(getDummyFile("/empty.txt").getAbsolutePath(), path, account.name);

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
