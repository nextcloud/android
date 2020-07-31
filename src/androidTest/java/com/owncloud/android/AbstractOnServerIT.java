package com.owncloud.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Common base for all integration tests
 */

public abstract class AbstractOnServerIT extends AbstractIT {
    @BeforeClass
    public static void beforeAll() {
        try {
            // clean up
            targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            AccountManager platformAccountManager = AccountManager.get(targetContext);

            for (Account account : platformAccountManager.getAccounts()) {
                if (account.type.equalsIgnoreCase("nextcloud")) {
                    platformAccountManager.removeAccountExplicitly(account);
                }
            }

            Bundle arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments();

            Uri baseUrl = Uri.parse(arguments.getString("TEST_SERVER_URL"));
            String loginName = arguments.getString("TEST_SERVER_USERNAME");
            String password = arguments.getString("TEST_SERVER_PASSWORD");

            Account temp = new Account(loginName + "@" + baseUrl, MainApp.getAccountType(targetContext));
            UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);
            if (!accountManager.exists(temp)) {
                platformAccountManager.addAccountExplicitly(temp, password, null);
                platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                                   Integer.toString(UserAccountManager.ACCOUNT_VERSION));
                platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
                platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, baseUrl.toString());
                platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, loginName); // same as userId
            }

            final UserAccountManager userAccountManager = UserAccountManagerImpl.fromContext(targetContext);
            account = userAccountManager.getAccountByName(loginName + "@" + baseUrl);

            if (account == null) {
                throw new ActivityNotFoundException();
            }

            Optional<User> optionalUser = userAccountManager.getUser(account.name);
            user = optionalUser.orElseThrow(IllegalAccessError::new);

            client = OwnCloudClientFactory.createOwnCloudClient(account, targetContext);

            createDummyFiles();

            waitForServer(client, baseUrl);

            deleteAllFiles(); // makes sure that no file/folder is in root

        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }
    }

    @After
    public void after() {
        deleteAllFiles();
    }

    public static void deleteAllFiles() {
        RemoteOperationResult result = new ReadFolderRemoteOperation("/").execute(client);
        assertTrue(result.getLogMessage(), result.isSuccess());

        for (Object object : result.getData()) {
            RemoteFile remoteFile = (RemoteFile) object;

            if (!remoteFile.getRemotePath().equals("/")) {
                if (remoteFile.isEncrypted()) {
                    assertTrue(new ToggleEncryptionRemoteOperation(remoteFile.getLocalId(),
                                                                   remoteFile.getRemotePath(),
                                                                   false)
                                   .execute(client)
                                   .isSuccess());
                }

                assertTrue(new RemoveFileRemoteOperation(remoteFile.getRemotePath())
                               .execute(client)
                               .isSuccess()
                          );
            }
        }
    }

    private static void waitForServer(OwnCloudClient client, Uri baseUrl) {
        GetMethod get = new GetMethod(baseUrl + "/status.php");

        try {
            int i = 0;
            while (client.executeMethod(get) != HttpStatus.SC_OK && i < 3) {
                System.out.println("wait…");
                Thread.sleep(60 * 1000);
                i++;
            }

            if (i == 3) {
                Assert.fail("Server not ready!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void uploadOCUpload(OCUpload ocUpload) {
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_COPY);
    }

    public void uploadOCUpload(OCUpload ocUpload, int localBehaviour) {
        ConnectivityService connectivityServiceMock = new ConnectivityService() {
            @Override
            public boolean isInternetWalled() {
                return false;
            }

            @Override
            public Connectivity getConnectivity() {
                return Connectivity.CONNECTED_WIFI;
            }
        };

        PowerManagementService powerManagementServiceMock = new PowerManagementService() {
            @NonNull
            @Override
            public BatteryStatus getBattery() {
                return new BatteryStatus();
            }

            @Override
            public boolean isPowerSavingEnabled() {
                return false;
            }

            @Override
            public boolean isPowerSavingExclusionAvailable() {
                return false;
            }
        };

        UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);
        UploadsStorageManager uploadsStorageManager = new UploadsStorageManager(accountManager,
                                                                                targetContext.getContentResolver());

        UploadFileOperation newUpload = new UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            powerManagementServiceMock,
            user,
            null,
            ocUpload,
            FileUploader.NameCollisionPolicy.DEFAULT,
            localBehaviour,
            targetContext,
            false,
            false
        );
        newUpload.addRenameUploadListener(() -> {
            // dummy
        });

        newUpload.setRemoteFolderToBeCreated();

        RemoteOperationResult result = newUpload.execute(client, getStorageManager());
        assertTrue(result.getLogMessage(), result.isSuccess());

        OCFile parentFolder = getStorageManager()
            .getFileByEncryptedRemotePath(new File(ocUpload.getRemotePath()).getParent() + "/");
        String uploadedFileName = new File(ocUpload.getRemotePath()).getName();
        OCFile uploadedFile = getStorageManager().
            getFileByDecryptedRemotePath(parentFolder.getDecryptedRemotePath() + uploadedFileName);

        assertNotNull(uploadedFile.getRemoteId());

        if (localBehaviour == FileUploader.LOCAL_BEHAVIOUR_COPY ||
            localBehaviour == FileUploader.LOCAL_BEHAVIOUR_MOVE) {
            assertTrue(new File(uploadedFile.getStoragePath()).exists());
        }
    }

    protected void refreshFolder(String path) {
        assertTrue(new RefreshFolderOperation(getStorageManager().getFileByEncryptedRemotePath(path),
                                              System.currentTimeMillis(),
                                              false,
                                              false,
                                              getStorageManager(),
                                              account,
                                              targetContext
        ).execute(client).isSuccess());
    }
}
