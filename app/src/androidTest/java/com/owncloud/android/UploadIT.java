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
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.model.GeoLocation;
import com.owncloud.android.lib.resources.files.model.ImageDimension;
import com.owncloud.android.lib.resources.status.NextcloudVersion;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests related to file uploads
 */

public class UploadIT extends AbstractOnServerIT {
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

    @Before
    public void before() throws IOException {
        // make sure that every file is available, even after tests that remove source file
        createDummyFiles();
    }

    @Test
    public void testEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "empty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testNonEmptyUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testUploadWithCopy() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_COPY);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");

        assertTrue(originalFile.exists());
        assertTrue(new File(uploadedFile.getStoragePath()).exists());
        verifyStoragePath(uploadedFile);
    }

    @Test
    public void testUploadWithMove() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_MOVE);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");

        assertFalse(originalFile.exists());
        assertTrue(new File(uploadedFile.getStoragePath()).exists());
        verifyStoragePath(uploadedFile);
    }

    @Test
    public void testUploadWithForget() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_FORGET);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");

        assertTrue(originalFile.exists());
        assertFalse(new File(uploadedFile.getStoragePath()).exists());
        assertTrue(uploadedFile.getStoragePath().isEmpty());
    }

    @Test
    public void testUploadWithDelete() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_DELETE);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(FOLDER + "nonEmpty.txt");

        assertFalse(originalFile.exists());
        assertFalse(new File(uploadedFile.getStoragePath()).exists());
        assertTrue(uploadedFile.getStoragePath().isEmpty());
    }

    @Test
    public void testChunkedUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/chunkedFile.txt",
                                         FOLDER + "chunkedFile.txt", account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testUploadInNonExistingFolder() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "2/3/4/1.txt", account.name);

        uploadOCUpload(ocUpload);
    }

    @Test
    public void testUploadOnChargingOnlyButNotCharging() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "notCharging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client);
        assertFalse(result.toString(), result.isSuccess());
        assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_CHARGING, result.getCode());
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

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "charging.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            true,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client);
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
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "noWifi.txt", account.name);
        ocUpload.setUseWifiOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client);
        assertFalse(result.toString(), result.isSuccess());
        assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI, result.getCode());
    }

    @Test
    public void testUploadOnWifiOnlyAndWifi() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "wifi.txt", account.name);
        ocUpload.setWhileChargingOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client);
        assertTrue(result.toString(), result.isSuccess());

        // cleanup
        new RemoveFileOperation(getStorageManager().getFileByDecryptedRemotePath(FOLDER),
                                false,
                                user,
                                false,
                                targetContext,
                                getStorageManager())
            .execute(client);
    }

    @Test
    public void testUploadOnWifiOnlyButMeteredWifi() {
        ConnectivityService connectivityServiceMock = new ConnectivityService() {
            @Override
            public boolean isInternetWalled() {
                return false;
            }

            @Override
            public Connectivity getConnectivity() {
                return new Connectivity(true, true, true, true);
            }
        };
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/empty.txt",
                                         FOLDER + "noWifi.txt",
                                         account.name);
        ocUpload.setUseWifiOnly(true);

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploader.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            true,
            false,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        RemoteOperationResult result = newUpload.execute(client);
        assertFalse(result.toString(), result.isSuccess());
        assertEquals(RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI, result.getCode());
    }

    @Test
    public void testCreationAndUploadTimestamp() throws IOException, AccountUtils.AccountNotFoundException {
        testOnlyOnServer(NextcloudVersion.nextcloud_27);

        File file = getDummyFile("empty.txt");
        String remotePath = "/testFile.txt";
        OCUpload ocUpload = new OCUpload(file.getAbsolutePath(), remotePath, account.name);

        assertTrue(
            new UploadFileOperation(
                uploadsStorageManager,
                connectivityServiceMock,
                powerManagementServiceMock,
                user,
                null,
                ocUpload,
                NameCollisionPolicy.DEFAULT,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                targetContext,
                false,
                false,
                getStorageManager()
            )
                .setRemoteFolderToBeCreated()
                .execute(client)
                .isSuccess()
                  );

        long creationTimestamp = Files.readAttributes(file.toPath(), BasicFileAttributes.class)
            .creationTime()
            .to(TimeUnit.SECONDS);

        long uploadTimestamp = System.currentTimeMillis() / 1000;

        // RefreshFolderOperation
        assertTrue(new RefreshFolderOperation(getStorageManager().getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              getStorageManager(),
                                              user,
                                              targetContext).execute(client).isSuccess());

        List<OCFile> files = getStorageManager().getFolderContent(getStorageManager().getFileByDecryptedRemotePath("/"),
                                                                  false);

        OCFile ocFile = files.get(0);

        assertEquals(remotePath, ocFile.getRemotePath());
        assertEquals(creationTimestamp, ocFile.getCreationTimestamp());
        assertTrue(uploadTimestamp - 10 < ocFile.getUploadTimestamp() ||
                           uploadTimestamp + 10 > ocFile.getUploadTimestamp());
    }

    @Test
    public void testMetadata() throws IOException, AccountUtils.AccountNotFoundException {
        testOnlyOnServer(NextcloudVersion.nextcloud_27);

        File file = getFile("gps.jpg");
        String remotePath = "/metadata.jpg";
        OCUpload ocUpload = new OCUpload(file.getAbsolutePath(), remotePath, account.name);

        assertTrue(
                new UploadFileOperation(
                        uploadsStorageManager,
                        connectivityServiceMock,
                        powerManagementServiceMock,
                        user,
                        null,
                        ocUpload,
                        NameCollisionPolicy.DEFAULT,
                        FileUploader.LOCAL_BEHAVIOUR_COPY,
                        targetContext,
                        false,
                        false,
                        getStorageManager()
                )
                        .setRemoteFolderToBeCreated()
                        .execute(client)
                        .isSuccess()
                  );

        // RefreshFolderOperation
        assertTrue(new RefreshFolderOperation(getStorageManager().getFileByDecryptedRemotePath("/"),
                                              System.currentTimeMillis() / 1000,
                                              false,
                                              false,
                                              getStorageManager(),
                                              user,
                                              targetContext).execute(client).isSuccess());

        List<OCFile> files = getStorageManager().getFolderContent(getStorageManager().getFileByDecryptedRemotePath("/"),
                                                                  false);

        OCFile ocFile = null;
        for (OCFile f : files) {
            if (f.getFileName().equals("metadata.jpg")) {
                ocFile = f;
                break;
            }
        }

        assertNotNull(ocFile);
        assertEquals(remotePath, ocFile.getRemotePath());
        assertEquals(new GeoLocation(64, -46), ocFile.getGeoLocation());
        assertEquals(new ImageDimension(300f, 200f), ocFile.getImageDimension());
    }

    private void verifyStoragePath(OCFile file) {
        assertEquals(FileStorageUtils.getSavePath(account.name) + FOLDER + file.getDecryptedFileName(),
                     file.getStoragePath());
    }
}
