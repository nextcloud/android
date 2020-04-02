/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android;

import android.content.ContentResolver;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

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
        public Connectivity getConnectivity() {
            return Connectivity.CONNECTED_WIFI;
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

        @NotNull
        @Override
        public BatteryStatus getBattery() {
            return new BatteryStatus(false, 0);
        }
    };

    @Before
    public void setUp() {
        final ContentResolver contentResolver = targetContext.getContentResolver();
        final UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);
        storageManager = new UploadsStorageManager(accountManager, contentResolver);
    }

    @Test
    public void testEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/empty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);

        // cleanup
        new RemoveFileOperation(new OCFile("/testUpload/"),
                                false,
                                account,
                                false,
                                targetContext)
            .execute(client, getStorageManager());
    }

    @Test
    public void testNonEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                         "/testUpload/nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);

        // cleanup
        new RemoveFileOperation(new OCFile("/testUpload/"),
                                false,
                                account,
                                false,
                                targetContext)
            .execute(client, getStorageManager());
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/chunkedFile.txt",
                                         "/testUpload/chunkedFile.txt", account.name);

        uploadOCUpload(ocUpload);

        // cleanup
        new RemoveFileOperation(new OCFile("/testUpload/"),
                                false,
                                account,
                                false,
                                targetContext)
            .execute(client, getStorageManager());
    }

    @Test
    public void testUploadInNonExistingFolder() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/2/3/4/1.txt", account.name);

        uploadOCUpload(ocUpload);

        // cleanup
        new RemoveFileOperation(new OCFile("/testUpload/"),
                                false,
                                account,
                                false,
                                targetContext)
            .execute(client, getStorageManager());
    }
}
