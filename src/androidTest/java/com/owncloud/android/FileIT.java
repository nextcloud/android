package com.owncloud.android;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.test.ApplicationTestCase;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.operations.common.SyncOperation;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by tobi on 3/2/18.
 */

public class FileIT extends ApplicationTestCase<MainApp> {

    OwnCloudClient client;
    Account account;

    public FileIT() {
        super(MainApp.class);
    }

    @BeforeClass
    public void beforeAll() {
        try {
            account = new Account("tobi@10.0.2.2/nc", MainApp.getAccountType());
            client = OwnCloudClientFactory.createOwnCloudClient(account, MainApp.getAppContext());
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateFolder() {
        beforeAll();

        SyncOperation syncOp = new CreateFolderOperation("/testIT/", true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());

        assertTrue(result.isSuccess());

        // file exists
        assertTrue(getStorageManager().getFileByPath("/testIT/").isFolder());
    }

    @Test
    public void testCreateNonExistingSubFolder() {
        beforeAll();

        SyncOperation syncOp = new CreateFolderOperation("/testIT/1/2", true);
        RemoteOperationResult result = syncOp.execute(client, getStorageManager());
        assertTrue(result.isSuccess());

        // file exists
        assertTrue(getStorageManager().getFileByPath("/testIT/1/2/").isFolder());
    }

    @Test
    public void testUpload() {
        beforeAll();

        OCUpload ocUpload = new OCUpload("/sdcard/1.txt", "/testIT/1.txt", account.name);
        UploadFileOperation newUpload = new UploadFileOperation(
                account,
                null,
                ocUpload,
                false,
                false,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                getContext(),
                false,
                false
        );
        newUpload.addRenameUploadListener(new UploadFileOperation.OnRenameListener() {
            @Override
            public void onRenameUpload() {
                // dummy
            }
        });

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.isSuccess());
    }


    private FileDataStorageManager getStorageManager() {
        return new FileDataStorageManager(account, getContext().getContentResolver());
    }
}
