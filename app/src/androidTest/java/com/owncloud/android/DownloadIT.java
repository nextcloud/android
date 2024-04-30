/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
 * Tests related to file uploads.
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
                                    user,
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
