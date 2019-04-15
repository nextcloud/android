package com.owncloud.android;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.common.SyncOperation;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Tests related to file operations
 */
@RunWith(AndroidJUnit4.class)
public class FileIT extends AbstractIT {

    @Test
    public void testCreateFolder() {
        String path = "/testFolder/";

        // folder does not exist yet
        assertNull(getStorageManager().getFileByPath(path));

        SyncOperation syncOp = new CreateFolderOperation(path, true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());

        assertTrue(result.toString(), result.isSuccess());

        // folder exists
        assertTrue(getStorageManager().getFileByPath(path).isFolder());

        // cleanup
        new RemoveFileOperation(path, false, account, false, targetContext).execute(client, getStorageManager());
    }

    @Test
    public void testCreateNonExistingSubFolder() {
        String path = "/testFolder/1/2/3/4/5/";
        // folder does not exist yet
        assertNull(getStorageManager().getFileByPath(path));

        SyncOperation syncOp = new CreateFolderOperation(path, true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());
        assertTrue(result.toString(), result.isSuccess());

        // folder exists
        assertTrue(getStorageManager().getFileByPath(path).isFolder());

        // cleanup
        new RemoveFileOperation("/testFolder/", false, account, false, targetContext).execute(client, getStorageManager());
    }
}
