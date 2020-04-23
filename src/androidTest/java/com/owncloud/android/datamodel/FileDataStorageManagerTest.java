/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;

import com.owncloud.android.lib.common.utils.Log_OC;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import androidx.test.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;

public class FileDataStorageManagerTest {

    private FileDataStorageManager storageManager;

    @Before
    public void setUp() {
        Context instrumentationCtx = InstrumentationRegistry.getTargetContext();
        ContentResolver contentResolver = instrumentationCtx.getContentResolver();
        Account account = new Account("A", "A");
        storageManager = new FileDataStorageManager(account, contentResolver);
    }

    @Test
    public void insertFile() {
        OCFile root = storageManager.getFileByPath("/");

        insertFiles(1);
        assertEquals(1, storageManager.getFolderContent(root, false).size());
    }

    @Test
    public void insertManyFilesAndDelete() {
        int count = 5000;
        insertFiles(count);

        OCFile root = storageManager.getFileByPath("/");
        assertEquals(count, storageManager.getFolderContent(root, false).size());

        // save folder and remove all files
        storageManager.saveFolder(root, new ArrayList<>(), storageManager.getFolderContent(root, false));
        assertEquals(0, storageManager.getFolderContent(root, false).size());
    }

    @Test
    public void insertManyFilesAndDelete2() {
        int count = 5000;
        insertFiles(count);

        OCFile root = storageManager.getFileByPath("/");
        assertEquals(count, storageManager.getFolderContent(root, false).size());

        storageManager.deleteAllFiles();
        assertEquals(0, storageManager.getFolderContent(root, false).size());
    }

    private void insertFiles(int count) {
        OCFile root = storageManager.getFileByPath("/");
        assertEquals(0, storageManager.getFolderContent(root, false).size());

        for (int i = 0; i < count; i++) {
            Log_OC.d(this, "insert: " + i);
            OCFile newFile = new OCFile("/" + i + ".txt");
            newFile.setRemoteId("oc12300" + i);
            newFile.setParentId(root.getFileId());

            storageManager.saveFile(newFile);
        }

        assertEquals(count, storageManager.getFolderContent(root, false).size());
    }

//    @After
//    public void after() {
//        OCFile root = storageManager.getFileByPath("/");
//        storageManager.deleteAllFiles();
//
//        assertEquals(0, storageManager.getFolderContent(root, false).size());
//    }
}
