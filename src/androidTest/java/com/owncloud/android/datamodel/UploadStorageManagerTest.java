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
import java.util.List;
import java.util.Random;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import static com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus.UPLOAD_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        assertEquals(0, uploadsStorageManager.getAllStoredUploads().size());

        for (int i = 0; i < size; i++) {
            OCUpload upload = createUpload(account);

            uploads.add(upload);
            uploadsStorageManager.storeUpload(upload);
        }

        List<OCUpload> storedUploads = uploadsStorageManager.getAllStoredUploads();
        assertEquals(size, uploadsStorageManager.getAllStoredUploads().size());

        for (int i = 0; i < size; i++) {
            assertTrue(contains(uploads, storedUploads.get(i)));
        }
    }

    @Test
    public void testIsSame() {
        OCUpload upload1 = new OCUpload("/test", "/test", account.name);
        upload1.setUseWifiOnly(true);
        OCUpload upload2 = new OCUpload("/test", "/test", account.name);
        upload2.setUseWifiOnly(true);

        assertTrue(upload1.isSame(upload2));

        upload2.setUseWifiOnly(false);
        assertFalse(upload1.isSame(upload2));

        assertFalse(upload1.isSame(null));
        assertFalse(upload1.isSame(new OCFile("/test")));
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

    @Test
    public void testConstraintsWithFailedUploads() {
        // no constraints
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(false));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setLastResult(UploadResult.LOCK_FAILED)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(false));

        // wifi only
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(false));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(false));

        // charging only
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(true));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(true));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(true));

        // wifi only, charging only
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(true));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(true));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(true));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setUseWifiOnly(true)
                                              .setWhileChargingOnly(true));

        // should not automatically retried
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setLastResult(UploadResult.SYNC_CONFLICT)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(false));

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED)
                                              .setLastResult(UploadResult.FILE_NOT_FOUND)
                                              .setUseWifiOnly(false)
                                              .setWhileChargingOnly(false));

        assertEquals(11, uploadsStorageManager.getFailedUploads().size());
        assertEquals(2, uploadsStorageManager.getFailedUploads(false, false).size());
        assertEquals(2, uploadsStorageManager.getFailedUploads(true, false).size());
        assertEquals(3, uploadsStorageManager.getFailedUploads(false, true).size());
        assertEquals(4, uploadsStorageManager.getFailedUploads(true, true).size());

        // scenario "no wifi, no charging"
        List<OCUpload> failedUploads;
        failedUploads = uploadsStorageManager.getFailedUploads(false, false);
        assertEquals(2, failedUploads.size());
        failedUploads.clear();

        // scenario "wifi, no charging"
        failedUploads = uploadsStorageManager.getFailedUploads(false, false);
        failedUploads.addAll(uploadsStorageManager.getFailedUploads(true, false));
        assertEquals(4, failedUploads.size());
        failedUploads.clear();

        // scenario "no wifi, charging"
        failedUploads = uploadsStorageManager.getFailedUploads(false, false);
        failedUploads.addAll(uploadsStorageManager.getFailedUploads(false, true));
        assertEquals(5, failedUploads.size());
        failedUploads.clear();

        // scenario "wifi, charging"
        failedUploads = uploadsStorageManager.getFailedUploads(false, false);
        failedUploads.addAll(uploadsStorageManager.getFailedUploads(true, false));
        failedUploads.addAll(uploadsStorageManager.getFailedUploads(false, true));
        failedUploads.addAll(uploadsStorageManager.getFailedUploads(true, true));
        assertEquals(11, failedUploads.size());
        failedUploads.clear();
    }

    @Test
    public void testDeleteUploadsWithOneValidAccount() {
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount1")
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount2")
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount3")
                                              .setUploadStatus(UPLOAD_FAILED));

        assertEquals(4, uploadsStorageManager.getFailedUploads().size());

        ArrayList<Account> accountList = new ArrayList<>();
        accountList.add(account);
        uploadsStorageManager.removeUploadsWithExpiredUsers(accountList);

        assertEquals(1, uploadsStorageManager.getFailedUploads().size());
    }

    @Test
    public void testDeleteUploadsWithTwoValidAccount() {
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account2.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount1")
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount2")
                                              .setUploadStatus(UPLOAD_FAILED));

        assertEquals(4, uploadsStorageManager.getFailedUploads().size());

        ArrayList<Account> accountList = new ArrayList<>();
        accountList.add(account);
        accountList.add(account2);
        uploadsStorageManager.removeUploadsWithExpiredUsers(accountList);

        assertEquals(2, uploadsStorageManager.getFailedUploads().size());
    }

    @Test
    public void testDeleteUploadsWithManyValidAccount() {
        Account account3 = new Account("test3@server.com", "Test-Type");

        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account2.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", account3.name)
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount1")
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount2")
                                              .setUploadStatus(UPLOAD_FAILED));
        uploadsStorageManager.storeUpload(new OCUpload("/test", "/test", "oldAccount3")
                                              .setUploadStatus(UPLOAD_FAILED));

        assertEquals(6, uploadsStorageManager.getFailedUploads().size());

        ArrayList<Account> accountList = new ArrayList<>();
        accountList.add(account);
        accountList.add(account2);
        accountList.add(account3);
        uploadsStorageManager.removeUploadsWithExpiredUsers(accountList);

        assertEquals(3, uploadsStorageManager.getFailedUploads().size());
    }

    private boolean contains(ArrayList<OCUpload> uploads, OCUpload storedUpload) {
        for (int i = 0; i < uploads.size(); i++) {
            if (storedUpload.isSame(uploads.get(i))) {
                return true;
            }
        }
        return false;
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
        uploadsStorageManager.removeAllUploads();

        AccountManager platformAccountManager = AccountManager.get(targetContext);
        platformAccountManager.removeAccountExplicitly(account2);
    }
}
