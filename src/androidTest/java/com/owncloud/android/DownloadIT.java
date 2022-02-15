/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android;

import android.net.Uri;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests related to file uploads
 */

public class DownloadIT extends AbstractOnServerIT {
    private static final String FOLDER = "/testUpload/";

    @After
    public void after() {
        RemoteOperationResult result = new RefreshFolderOperation(getStorageManager().getFileByPath("/"),
                                                                  System.currentTimeMillis() / 1000L,
                                                                  false,
                                                                  true,
                                                                  getStorageManager(),
                                                                  user,
                                                                  targetContext)
            .execute(client);

        // cleanup only if folder exists
        if (result.isSuccess() && getStorageManager().getFileByDecryptedRemotePath(FOLDER) != null) {
            new RemoveFileOperation(getStorageManager().getFileByDecryptedRemotePath(FOLDER),
                                    false,
                                    account,
                                    false,
                                    targetContext,
                                    getStorageManager())
                .execute(client);
        }
    }

    @Test
    public void verifyDownload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);

        OCUpload ocUpload2 = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                          FOLDER + "nonEmpty2.txt",
                                          account.name);

        uploadOCUpload(ocUpload2);

        refreshFolder("/");
        refreshFolder(FOLDER);

        OCFile file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");
        OCFile file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt");
        verifyDownload(file1, file2);

        assertTrue(new DownloadFileOperation(user, file1, targetContext).execute(client).isSuccess());
        assertTrue(new DownloadFileOperation(user, file2, targetContext).execute(client).isSuccess());

        refreshFolder(FOLDER);

        file1 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");
        file2 = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty2.txt");

        verifyDownload(file1, file2);
    }

    private void verifyDownload(OCFile file1, OCFile file2) {
        assertNotNull(file1);
        assertNotNull(file2);
        assertNotSame(file1.getStoragePath(), file2.getStoragePath());

        assertTrue(new File(file1.getStoragePath()).exists());
        assertTrue(new File(file2.getStoragePath()).exists());

        // test against hardcoded path to make sure that it is correct
        assertEquals("/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/" +
                         Uri.encode(account.name, "@") + "/testUpload/nonEmpty.txt",
                     file1.getStoragePath());
        assertEquals("/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/" +
                         Uri.encode(account.name, "@") + "/testUpload/nonEmpty2.txt",
                     file2.getStoragePath());
    }
}
