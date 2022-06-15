package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.MainApp;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.NameCollisionPolicy;
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

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by JARP on 6/7/17.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UploadStorageManagerTest extends AbstractIT {
    private UploadsStorageManager uploadsStorageManager;
    private CurrentAccountProvider currentAccountProvider = () -> null;
    private UserAccountManager userAccountManager;
    private User user2;

    @Before
    public void setUp() {
        Context instrumentationCtx = ApplicationProvider.getApplicationContext();
        ContentResolver contentResolver = instrumentationCtx.getContentResolver();
        uploadsStorageManager = new UploadsStorageManager(currentAccountProvider, contentResolver);
        userAccountManager = UserAccountManagerImpl.fromContext(targetContext);

        Account temp = new Account("test2@test.com", MainApp.getAccountType(targetContext));
        if (!userAccountManager.exists(temp)) {
            AccountManager platformAccountManager = AccountManager.get(targetContext);
            platformAccountManager.addAccountExplicitly(temp, "testPassword", null);
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                               Integer.toString(UserAccountManager.ACCOUNT_VERSION));
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "test.com");
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "test"); // same as userId
        }

        final UserAccountManager userAccountManager = UserAccountManagerImpl.fromContext(targetContext);
        user2 = userAccountManager.getUser("test2@test.com").orElseThrow(ActivityNotFoundException::new);
    }

    @Test
    public void testDeleteAllUploads() {
        // Clean
        for (User user : userAccountManager.getAllUsers()) {
            uploadsStorageManager.removeUserUploads(user);
        }
        int accountRowsA = 3;
        int accountRowsB = 4;
        insertUploads(account, accountRowsA);
        insertUploads(user2.toPlatformAccount(), accountRowsB);

        assertEquals("Expected 4 removed uploads files",
                     4,
                     uploadsStorageManager.removeUserUploads(user2));
    }

    @Test
    public void largeTest() {
        int size = 3000;
        ArrayList<OCUpload> uploads = new ArrayList<>();

        deleteAllUploads();
        assertEquals(0, uploadsStorageManager.getAllStoredUploads().length);

        for (int i = 0; i < size; i++) {
            OCUpload upload = createUpload(account);

            uploads.add(upload);
            uploadsStorageManager.storeUpload(upload);
        }

        OCUpload[] storedUploads = uploadsStorageManager.getAllStoredUploads();
        assertEquals(size, uploadsStorageManager.getAllStoredUploads().length);

        for (int i = 0; i < size; i++) {
            assertTrue(contains(uploads, storedUploads[i]));
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

    private boolean contains(ArrayList<OCUpload> uploads, OCUpload storedUpload) {
        for (int i = 0; i < uploads.size(); i++) {
            if (storedUpload.isSame(uploads.get(i))) {
                return true;
            }
        }
        return false;
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
    public void getById() {
        OCUpload upload = createUpload(account);
        long id = uploadsStorageManager.storeUpload(upload);

        OCUpload newUpload = uploadsStorageManager.getUploadById(id);

        assertNotNull(newUpload);
        assertEquals(upload.getLocalAction(), newUpload.getLocalAction());
        assertEquals(upload.getFolderUnlockToken(), newUpload.getFolderUnlockToken());
    }

    @Test
    public void getByIdNull() {
        OCUpload newUpload = uploadsStorageManager.getUploadById(-1);

        assertNull(newUpload);
    }

    private void insertUploads(Account account, int rowsToInsert) {
        for (int i = 0; i < rowsToInsert; i++) {
            uploadsStorageManager.storeUpload(createUpload(account));
        }
    }

    private OCUpload createUpload(Account account) {
        OCUpload upload = new OCUpload(File.separator + "very long long long long long long long long long long long " +
                                           "long long long long long long long long long long long long long long " +
                                           "long long long long long long long long long long long long long long " +
                                           "long long long long long long long LocalPath",
                                       OCFile.PATH_SEPARATOR + "very long long long long long long long long long " +
                                           "long long long long long long long long long long long long long long " +
                                           "long long long long long long long long long long long long long long " +
                                           "long long long long long long long long long long long long RemotePath",
                                       account.name);

        upload.setFileSize(new Random().nextInt(20000) * 10000);
        upload.setUploadStatus(UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS);
        upload.setLocalAction(2);
        upload.setNameCollisionPolicy(NameCollisionPolicy.ASK_USER);
        upload.setCreateRemoteFolder(false);
        upload.setUploadEndTimestamp(System.currentTimeMillis());
        upload.setLastResult(UploadResult.DELAYED_FOR_WIFI);
        upload.setCreatedBy(UploadFileOperation.CREATED_BY_USER);
        upload.setUseWifiOnly(true);
        upload.setWhileChargingOnly(false);
        upload.setFolderUnlockToken(RandomString.make(10));

        return upload;
    }

    private void deleteAllUploads() {
        uploadsStorageManager.removeAllUploads();

        assertEquals(0, uploadsStorageManager.getAllStoredUploads().length);
    }

    @After
    public void tearDown() {
        deleteAllUploads();
        userAccountManager.removeUser(user2);
    }
}
