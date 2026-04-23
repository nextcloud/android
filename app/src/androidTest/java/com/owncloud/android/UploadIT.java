/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android;

import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
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
import com.owncloud.android.utils.MimeType;

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
 * Tests related to file uploads.
 */
public class UploadIT extends AbstractOnServerIT {
    private static final String FOLDER = "/testUpload/";

    private UploadsStorageManager uploadsStorageManager =
        new UploadsStorageManager(UserAccountManagerImpl.fromContext(targetContext),
                                  targetContext.getContentResolver());

    private ConnectivityService connectivityServiceMock = new ConnectivityService() {
        @Override
        public void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback) {

        }

        @Override
        public boolean isConnected() {
            return false;
        }

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

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_COPY);

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

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_MOVE);

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

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_FORGET);

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

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_DELETE);

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
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
            public void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback) {

            }

            @Override
            public boolean isConnected() {
                return false;
            }

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
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
            public void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback) {

            }

            @Override
            public boolean isConnected() {
                return false;
            }

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
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
                FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
        assertTrue(uploadTimestamp - 10 < ocFile.getUploadTimestamp() &&
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
                    FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
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
            if ("metadata.jpg".equals(f.getFileName())) {
                ocFile = f;
                break;
            }
        }

        assertNotNull(ocFile);
        assertEquals(remotePath, ocFile.getRemotePath());
        assertEquals(new GeoLocation(64, -46), ocFile.getGeoLocation());
        assertEquals(new ImageDimension(300f, 200f), ocFile.getImageDimension());
    }

    @Test
    public void testEncryptedUploadCallsEncryptedUploadNotNormalUpload() {
        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         FOLDER + "nonEmpty.txt",
                                         account.name);

        OCFile encryptedFile = new OCFile(FOLDER + "nonEmpty.txt");
        encryptedFile.setEncrypted(true);
        encryptedFile.setStoragePath(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            encryptedFile,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> {});

        newUpload.execute(client);

        assertNotNull(newUpload.getDecryptedRemotePath());
        assertFalse(newUpload.getDecryptedRemotePath().isEmpty());
        assertTrue(newUpload.getFile().isEncrypted());
    }

    @Test
    public void testEncryptedAncestorTriggerEncryptedUpload() {
        OCFile encryptedParentFolder = new OCFile(FOLDER);
        encryptedParentFolder.setMimeType(MimeType.DIRECTORY);
        encryptedParentFolder.setEncrypted(true);
        encryptedParentFolder.setDecryptedRemotePath(FOLDER);
        encryptedParentFolder.setParentId(getStorageManager().getFileByDecryptedRemotePath("/").getFileId());
        getStorageManager().saveFile(encryptedParentFolder);

        // Reload so we get the assigned fileId back from the DB
        encryptedParentFolder = getStorageManager().getFileByDecryptedRemotePath(FOLDER);
        assertNotNull("Encrypted parent folder must exist in storage manager", encryptedParentFolder);
        assertTrue("Parent folder must be marked encrypted", encryptedParentFolder.isEncrypted());

        // Create the child file pointing at the encrypted parent
        OCFile childFile = new OCFile(FOLDER + "nonEmpty.txt");
        childFile.setStoragePath(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        childFile.setParentId(encryptedParentFolder.getFileId());
        childFile.setEncrypted(false); // explicitly NOT encrypted itself; ancestor is
        getStorageManager().saveFile(childFile);

        OCUpload ocUpload = new OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            FOLDER + "nonEmpty.txt",
            account.name
        );

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            childFile,
            ocUpload,
            NameCollisionPolicy.DEFAULT,
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            targetContext,
            false,
            false,
            getStorageManager()
        );
        newUpload.setRemoteFolderToBeCreated();
        newUpload.addRenameUploadListener(() -> { });

        newUpload.execute(client);

        assertTrue(
            "mFile must be marked encrypted, proving encryptedUpload() was called not normalUpload()",
            newUpload.getFile().isEncrypted()
                  );

        assertNotNull(
            "decryptedRemotePath must be set, which only encryptedUpload() does",
            newUpload.getDecryptedRemotePath()
                     );
        assertFalse(
            "decryptedRemotePath must be non-empty, which only encryptedUpload() does",
            newUpload.getDecryptedRemotePath().isEmpty()
                   );
    }

    private void verifyStoragePath(OCFile file) {
        assertEquals(FileStorageUtils.getSavePath(account.name) + FOLDER + file.getDecryptedFileName(),
                     file.getStoragePath());
    }
}
