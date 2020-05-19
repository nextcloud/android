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
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests related to file uploads
 */

@RunWith(AndroidJUnit4.class)
public class UploadIT extends AbstractIT {

    private UploadsStorageManager storageManager =
        new UploadsStorageManager(UserAccountManagerImpl.fromContext(targetContext),
                                  targetContext.getContentResolver());

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
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext)
            .execute(client, getStorageManager());
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/chunkedFile.txt",
                                         "/testUpload/chunkedFile.txt", account.name);

        uploadOCUpload(ocUpload);

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext)
            .execute(client, getStorageManager());
    }

    public RemoteOperationResult testUpload(OCUpload ocUpload) {
        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
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
    public void testUploadOnChargingOnlyButNotCharging() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/notCharging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertFalse(result.toString(), result.isSuccess());
    }

    @Test
    public void testUploadOnChargingOnlyAndCharging() {
        PowerManagementService powerManagementServiceMock = new PowerManagementService() {
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
                return new BatteryStatus(true, 100);
            }
        };

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/charging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext)
            .execute(client, getStorageManager());
    }

    @Test
    public void testUploadOnWifiOnlyButNoWifi() {
        ConnectivityService connectivityServiceMock = new ConnectivityService() {
            @Override
            public boolean isInternetWalled() {
                return false;
            }

            @Override
            public Connectivity getConnectivity() {
                return new Connectivity(true, false, false, true);
            }
        };
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/noWifi.txt", account.name);
        ocUpload.setUseWifiOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertFalse(result.toString(), result.isSuccess());
    }

    @Test
    public void testUploadOnWifiOnlyAndWifi() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         "/testUpload/wifi.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            storageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            account,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation("/testUpload/", false, account, false, targetContext)
            .execute(client, getStorageManager());
    }
}
