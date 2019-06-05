package com.owncloud.android;

import android.content.ContentResolver;

import com.evernote.android.job.JobRequest;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertTrue;

/**
 * Tests related to file uploads
 */

@RunWith(AndroidJUnit4.class)
public class UploadIT extends AbstractIT {

    private UploadsStorageManager storageManager;

    private ConnectivityService connectivityServiceMock = new ConnectivityService() {
        @Override
        public boolean isInternetWalled() {
            return false;
        }

        @Override
        public boolean isOnlineWithWifi() {
            return true;
        }

        @Override
        public JobRequest.NetworkType getActiveNetworkType() {
            return JobRequest.NetworkType.ANY;
        }
    };

    private PowerManagementService powerManagementServiceMock = new PowerManagementService() {
        @Override
        public boolean isPowerSavingEnabled() {
            return false;
        }

        @Override
        public boolean isPowerSavingExclusionAvailable() {
            return false;
        }

        @Override
        public boolean isBatteryCharging() {
            return false;
        }
    };

    @Before
    public void setUp() {
        final ContentResolver contentResolver = targetContext.getContentResolver();
        final CurrentAccountProvider currentAccountProvider = () -> AccountUtils.getCurrentOwnCloudAccount(targetContext);
        storageManager = new UploadsStorageManager(currentAccountProvider, contentResolver);
    }

    @Test
    public void testEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
            "/testUpload/empty.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext).execute(client, getStorageManager());
    }

    @Test
    public void testNonEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
            "/testUpload/nonEmpty.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext).execute(client, getStorageManager());
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/chunkedFile.txt",
            "/testUpload/chunkedFile.txt", account.name);

        RemoteOperationResult result = testUpload(ocUpload);

        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext).execute(client, getStorageManager());
    }

    public RemoteOperationResult testUpload(OCUpload ocUpload) {
        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            false,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
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
                storageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                account,
                null,
                ocUpload,
                false,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false
        );
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        newUpload.setRemoteFolderToBeCreated();

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext).execute(client, getStorageManager());
    }
}
