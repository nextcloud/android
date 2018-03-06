package com.owncloud.android;

import android.support.test.runner.AndroidJUnit4;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to file uploads
 */

@RunWith(AndroidJUnit4.class)
public class UploadIT extends AbstractIT {

    @Test
    public void testSimpleUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/123.txt",
                "/testUpload/1.txt", account.name);
        UploadFileOperation newUpload = new UploadFileOperation(
                account,
                null,
                ocUpload,
                false,
                false,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                context,
                false,
                false
        );
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        newUpload.setRemoteFolderToBeCreated();

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.isSuccess());
    }

    @Test
    public void testUploadInNonExistingFolder() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/123.txt",
                "/testUpload/2/3/4/1.txt", account.name);
        UploadFileOperation newUpload = new UploadFileOperation(
                account,
                null,
                ocUpload,
                false,
                false,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                context,
                false,
                false
        );
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        newUpload.setRemoteFolderToBeCreated();

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.isSuccess());
    }
}
