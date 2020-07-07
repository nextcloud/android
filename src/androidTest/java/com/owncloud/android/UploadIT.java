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
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests related to file uploads
 */

@RunWith(AndroidJUnit4.class)
public class UploadIT extends AbstractIT {
    private static final String FOLDER = "/testUpload/";

    private UploadsStorageManager uploadsStorageManager =
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

        @NonNull
        @Override
        public BatteryStatus getBattery() {
            return new BatteryStatus(false, 0);
        }
    };

    @After
    public void after() {
        RemoteOperationResult result = new RefreshFolderOperation(getStorageManager().getFileByPath("/"),
                                                                  System.currentTimeMillis() / 1000L,
                                                                  false,
                                                                  true,
                                                                  getStorageManager(),
                                                                  account,
                                                                  targetContext)
            .execute(client);

        // cleanup only if folder exists
        if (result.isSuccess() && getStorageManager().getFileByDecryptedRemotePath(FOLDER) != null) {
            new RemoveFileOperation(getStorageManager().getFileByDecryptedRemotePath(FOLDER),
                                    false,
                                    account,
                                    false,
                                    targetContext)
                .execute(client, getStorageManager());
        }
    }

    @Test
    public void testEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         FOLDER + "empty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testNonEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/chunkedFile.txt",
                                         FOLDER + "chunkedFile.txt", account.name);

        uploadOCUpload(ocUpload);
    }

    public RemoteOperationResult testUpload(OCUpload ocUpload) {
        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
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
                                         FOLDER + "2/3/4/1.txt", account.name);

        uploadOCUpload(ocUpload);
    }
    @Test
    public void testUploadOnChargingOnlyButNotCharging() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         FOLDER + "notCharging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
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

            @NonNull
            @Override
            public BatteryStatus getBattery() {
                return new BatteryStatus(true, 100);
            }
        };

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + "/empty.txt",
                                         FOLDER + "charging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
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
                                         FOLDER + "noWifi.txt", account.name);
        ocUpload.setUseWifiOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
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
                                         FOLDER + "wifi.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
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
        new RemoveFileOperation(getStorageManager().getFileByDecryptedRemotePath(FOLDER),
                                false,
                                account,
                                false,
                                targetContext)
            .execute(client, getStorageManager());
    }
}
