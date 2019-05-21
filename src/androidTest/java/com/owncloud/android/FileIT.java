package com.owncloud.android;

import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.common.SyncOperation;

import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

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
    public void testMkCol() throws IOException, InterruptedException {
        int READ_TIMEOUT = 30000;
        int CONNECTION_TIMEOUT = 5000;

        MkColMethod mkCol1 = new MkColMethod(client.getWebdavUri() + WebdavUtils.encodePath("/mkcol"));
        client.executeMethod(mkCol1, READ_TIMEOUT, CONNECTION_TIMEOUT);
        mkCol1.releaseConnection();

        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            System.out.println("mkcol start: " + i);
            MkColMethod mkCol = new MkColMethod(client.getWebdavUri() + WebdavUtils.encodePath("/mkcol/" + i));
            client.executeMethod(mkCol, READ_TIMEOUT, CONNECTION_TIMEOUT);

            mkCol.releaseConnection();

            if (!mkCol.succeeded()) {
                System.out.println(mkCol.getResponseBodyAsString());
            }

            long end = System.currentTimeMillis();
            System.out.println("mkcol end: " + i + " time: " + (end - start) / 1000l);
        }

        DeleteMethod delete = new DeleteMethod(client.getWebdavUri() + WebdavUtils.encodePath("/mkcol"));
        client.executeMethod(delete, READ_TIMEOUT, CONNECTION_TIMEOUT);
        delete.releaseConnection();
    }

    @Test
    public void testCreateNonExistingSubFolder() {
        String path = "/testFolder/1/2/3/4/5/6/7/8/9/10/";
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
