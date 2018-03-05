package com.owncloud.android;

import android.support.test.runner.AndroidJUnit4;

import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by tobi on 3/2/18.
 */

@RunWith(AndroidJUnit4.class)
public class FileIT extends AbstractIT {

    @Test
    public void testCreateFolder() {
        SyncOperation syncOp = new CreateFolderOperation("/testIT/", true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());

        assertTrue(result.isSuccess());

        // file exists
        assertTrue(getStorageManager().getFileByPath("/testIT/").isFolder());
    }

    @Test
    public void testCreateNonExistingSubFolder() {
        SyncOperation syncOp = new CreateFolderOperation("/testIT/1/2", true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());
        assertTrue(result.isSuccess());

        // file exists
        assertTrue(getStorageManager().getFileByPath("/testIT/1/2/").isFolder());
    }

    @Test
    public void testUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/123.txt",
                "/testIT/1.txt", account.name);
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

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.isSuccess());
    }

    @Test
    public void testUploadInNonExistingFolder() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/123.txt",
                "/testIT/2/3/4/1.txt", account.name);
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
