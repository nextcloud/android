package com.owncloud.android;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.runner.AndroidJUnit4;

import static junit.framework.TestCase.assertTrue;

/**
 * Tests related to file uploads
 */

@RunWith(AndroidJUnit4.class)
public class UploadIT extends AbstractIT {

    @Test
    public void testEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
            "/testUpload/empty.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testNonEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
            "/testUpload/nonEmpty.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.isSuccess());
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/chunkedFile.txt",
            "/testUpload/chunkedFile.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.isSuccess());
    }

    public RemoteOperationResult testUpload(OCUpload ocUpload) {
        UploadFileOperation newUpload = new UploadFileOperation(
            account,
            null,
            ocUpload,
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

        return newUpload.execute(client, getStorageManager());
    }

    @Test
    public void testUploadInNonExistingFolder() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                "/testUpload/2/3/4/1.txt", account.name);
        UploadFileOperation newUpload = new UploadFileOperation(
                account,
                null,
                ocUpload,
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
