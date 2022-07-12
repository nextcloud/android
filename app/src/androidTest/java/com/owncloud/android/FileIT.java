package com.owncloud.android;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.operations.common.SyncOperation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Tests related to file operations
 */
@RunWith(AndroidJUnit4.class)
public class FileIT extends AbstractOnServerIT {

    @Test
    public void testCreateFolder() {
        String path = "/testFolder/";

        // folder does not exist yet
        assertNull(getStorageManager().getFileByPath(path));

        SyncOperation syncOp = new CreateFolderOperation(path, user, targetContext, getStorageManager());
        RemoteOperationResult result = syncOp.execute(client);

        assertTrue(result.toString(), result.isSuccess());

        // folder exists
        OCFile file = getStorageManager().getFileByPath(path);
        assertTrue(file.isFolder());

        // cleanup
        assertTrue(new RemoveFileOperation(file, false, user, false, targetContext, getStorageManager())
                       .execute(client)
                       .isSuccess());
    }

    @Test
    public void testCreateNonExistingSubFolder() {
        String path = "/subFolder/1/2/3/4/5/";
        // folder does not exist yet
        assertNull(getStorageManager().getFileByPath(path));

        SyncOperation syncOp = new CreateFolderOperation(path, user, targetContext, getStorageManager());
        RemoteOperationResult result = syncOp.execute(client);
        assertTrue(result.toString(), result.isSuccess());

        // folder exists
        OCFile file = getStorageManager().getFileByPath(path);
        assertTrue(file.isFolder());

        // cleanup
        new RemoveFileOperation(file,
                                false,
                                user,
                                false,
                                targetContext,
                                getStorageManager())
            .execute(client);
    }

    @Test
    public void testRemoteIdNull() {
        getStorageManager().deleteAllFiles();
        assertEquals(0, getStorageManager().getAllFiles().size());

        OCFile test = new OCFile("/123.txt");
        getStorageManager().saveFile(test);
        assertEquals(1, getStorageManager().getAllFiles().size());

        getStorageManager().deleteAllFiles();
        assertEquals(0, getStorageManager().getAllFiles().size());
    }

    @Test
    public void testRenameFolder() throws IOException {
        String folderPath = "/testRenameFolder/";

        // create folder
        createFolder(folderPath);

        // upload file inside it
        uploadFile(getDummyFile("nonEmpty.txt"), folderPath + "text.txt");

        // sync folder
        assertTrue(new SynchronizeFolderOperation(targetContext,
                                                  folderPath,
                                                  user,
                                                  System.currentTimeMillis(),
                                                  fileDataStorageManager)
                       .execute(targetContext)
                       .isSuccess());

        // check if file exists
        String storagePath1 = fileDataStorageManager.getFileByDecryptedRemotePath(folderPath).getStoragePath();
        assertTrue(new File(storagePath1).exists());

        String storagePath2 = fileDataStorageManager
            .getFileByDecryptedRemotePath(folderPath + "text.txt")
            .getStoragePath();
        assertTrue(new File(storagePath2).exists());

        shortSleep();

        // Rename
        assertTrue(
            new RenameFileOperation(folderPath, "test123", fileDataStorageManager)
                .execute(targetContext)
                .isSuccess()
                  );

        // after rename check new location
        assertTrue(
            new File(fileDataStorageManager.getFileByDecryptedRemotePath("/test123/").getStoragePath())
                .exists()
                  );
        assertTrue(
            new File(fileDataStorageManager.getFileByDecryptedRemotePath("/test123/text.txt").getStoragePath())
                .exists()
                  );

        // old files do no exist
        assertNull(fileDataStorageManager.getFileByDecryptedRemotePath(folderPath));
        assertNull(fileDataStorageManager.getFileByDecryptedRemotePath(folderPath + "text.txt"));

        // local files also do not exist
        assertFalse(new File(storagePath1).exists());
        assertFalse(new File(storagePath2).exists());
    }
}
