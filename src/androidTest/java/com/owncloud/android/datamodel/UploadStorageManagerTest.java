package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.owncloud.android.db.OCUpload;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Created by JARP on 6/7/17.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UploadStorageManagerTest {

    private Account[] Accounts;
    private UploadsStorageManager uploadsStorageManager;

    @Before
    public void setUp() {
        Context instrumentationCtx = InstrumentationRegistry.getTargetContext();
        ContentResolver contentResolver = instrumentationCtx.getContentResolver();
        uploadsStorageManager = new UploadsStorageManager(contentResolver, instrumentationCtx);
        Accounts = new Account[]{new Account("A", "A"), new Account("B", "B")};
    }

    @Test
    public void testDeleteAllUploads() {
        // Clean
        for (Account account : Accounts) {
            uploadsStorageManager.removeAccountUploads(account);
        }
        int accountRowsA = 3;
        int accountRowsB = 4;
        insertUploads(Accounts[0], accountRowsA);
        insertUploads(Accounts[1], accountRowsB);

        Assert.assertTrue("Expected 4 removed uploads files", uploadsStorageManager.removeAccountUploads(Accounts[1]) == 4);
    }

    private void insertUploads(Account account, int rowsToInsert) {

        for (int i = 0; i < rowsToInsert; i++) {
            uploadsStorageManager.storeUpload(createUpload(account));
        }
    }

    private OCUpload createUpload(Account acc) {
        return new OCUpload(File.separator + "LocalPath",
                OCFile.PATH_SEPARATOR + "RemotePath",
                acc.name);
    }

    @After
    public void tearDown() {
        for (Account account : Accounts) {
            uploadsStorageManager.removeAccountUploads(account);
        }
    }
}
