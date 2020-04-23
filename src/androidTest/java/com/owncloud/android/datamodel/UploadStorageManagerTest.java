package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.MainApp;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.operations.UploadFileOperation;

import net.bytebuddy.utility.RandomString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by JARP on 6/7/17.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UploadStorageManagerTest extends AbstractIT {
    private UploadsStorageManager uploadsStorageManager;
    private CurrentAccountProvider currentAccountProvider = () -> null;
    private Account account2;

    @Before
    public void setUp() {
        Context instrumentationCtx = InstrumentationRegistry.getTargetContext();
        ContentResolver contentResolver = instrumentationCtx.getContentResolver();
        uploadsStorageManager = new UploadsStorageManager(currentAccountProvider, contentResolver);

        Account temp = new Account("test2@test.com", MainApp.getAccountType(targetContext));
        UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);
        if (!accountManager.exists(temp)) {
            AccountManager platformAccountManager = AccountManager.get(targetContext);
            platformAccountManager.addAccountExplicitly(temp, "testPassword", null);
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                               Integer.toString(UserAccountManager.ACCOUNT_VERSION));
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "test.com");
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "test"); // same as userId
        }

        final UserAccountManager userAccountManager = UserAccountManagerImpl.fromContext(targetContext);
        account2 = userAccountManager.getAccountByName("test2@test.com");

        if (account2 == null) {
            throw new ActivityNotFoundException();
        }
    }

    @Test
    public void testDeleteAllUploads() {
        // Clean
        for (Account account : getAllAccounts()) {
            uploadsStorageManager.removeAccountUploads(account);
        }
        int accountRowsA = 3;
        int accountRowsB = 4;
        insertUploads(account, accountRowsA);
        insertUploads(account2, accountRowsB);

        assertEquals("Expected 4 removed uploads files",
                     4,
                     uploadsStorageManager.removeAccountUploads(account2));
    }

    @Test
    public void largeTest() {
        int size = 3000;
        ArrayList<OCUpload> uploads = new ArrayList<>();

        assertEquals(0, uploadsStorageManager.getAllStoredUploads().length);

        for (int i = 0; i < size; i++) {
            OCUpload upload = createUpload(account);

            uploads.add(upload);
            uploadsStorageManager.storeUpload(upload);
        }

        OCUpload[] storedUploads = uploadsStorageManager.getAllStoredUploads();
        assertEquals(size, uploadsStorageManager.getAllStoredUploads().length);

        for (int i = 0; i < size; i++) {
            assertTrue(uploads.contains(storedUploads[i]));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void corruptedUpload() {
        OCUpload corruptUpload = new OCUpload(File.separator + "LocalPath",
                                              OCFile.PATH_SEPARATOR + "RemotePath",
                                              account.name);

        corruptUpload.setLocalPath(null);

        uploadsStorageManager.storeUpload(corruptUpload);

        uploadsStorageManager.getAllStoredUploads();
    }

    private void insertUploads(Account account, int rowsToInsert) {
        for (int i = 0; i < rowsToInsert; i++) {
            uploadsStorageManager.storeUpload(createUpload(account));
        }
    }

    private OCUpload createUpload(Account account) {
        OCUpload upload = new OCUpload(File.separator + "very long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long LocalPath",
                                       OCFile.PATH_SEPARATOR + "very long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long RemotePath",
                                       account.name);

        upload.setFileSize(new Random().nextInt(20000) * 10000);
        upload.setUploadStatus(UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS);
        upload.setLocalAction(2);
        upload.setNameCollisionPolicy(FileUploader.NameCollisionPolicy.ASK_USER);
        upload.setCreateRemoteFolder(false);
        upload.setUploadEndTimestamp(System.currentTimeMillis());
        upload.setLastResult(UploadResult.DELAYED_FOR_WIFI);
        upload.setCreatedBy(UploadFileOperation.CREATED_BY_USER);
        upload.setUseWifiOnly(true);
        upload.setWhileChargingOnly(false);
        upload.setFolderUnlockToken(RandomString.make(10));

        return upload;
    }

    @After
    public void tearDown() {
        for (Account account : getAllAccounts()) {
            uploadsStorageManager.removeAccountUploads(account);
        }
    }
}
