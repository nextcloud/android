package com.owncloud.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;

import com.facebook.testing.screenshot.Screenshot;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.device.BatteryStatus;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.Connectivity;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import androidx.annotation.NonNull;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;


/**
 * Common base for all integration tests
 */

public abstract class AbstractIT {
    //@Rule public RetryTestRule retryTestRule = new RetryTestRule();

    protected static OwnCloudClient client;
    protected static Account account;
    protected static User user;
    protected static Context targetContext;

    protected Activity currentActivity;

    protected FileDataStorageManager fileDataStorageManager =
        new FileDataStorageManager(account, targetContext.getContentResolver());

    @BeforeClass
    public static void beforeAll() {
        try {
            // clean up
            targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            AccountManager platformAccountManager = AccountManager.get(targetContext);

            for (Account account : platformAccountManager.getAccounts()) {
                platformAccountManager.removeAccountExplicitly(account);
            }

            Account temp = new Account("test@https://server.com", MainApp.getAccountType(targetContext));
            platformAccountManager.addAccountExplicitly(temp, "password", null);
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "https://server.com");
            platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "test");

            final UserAccountManager userAccountManager = UserAccountManagerImpl.fromContext(targetContext);
            account = userAccountManager.getAccountByName("test@https://server.com");

            if (account == null) {
                throw new ActivityNotFoundException();
            }

            Optional<User> optionalUser = userAccountManager.getUser(account.name);
            user = optionalUser.orElseThrow(IllegalAccessError::new);

            client = OwnCloudClientFactory.createOwnCloudClient(account, targetContext);
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

    protected FileDataStorageManager getStorageManager() {
        return fileDataStorageManager;
    }

    protected Account[] getAllAccounts() {
        return AccountManager.get(targetContext).getAccounts();
    }

    public static void createFile(String name, int iteration) throws IOException {
        File file = new File(FileStorageUtils.getSavePath(account.name) + File.separator + name);
        file.createNewFile();

        FileWriter writer = new FileWriter(file);

        for (int i = 0; i < iteration; i++) {
            writer.write("123123123123123123123123123\n");
        }
        writer.flush();
        writer.close();
    }

    protected File getFile(String filename) throws IOException {
        InputStream inputStream = getInstrumentation().getContext().getAssets().open(filename);
        File temp = File.createTempFile("file", "file");
        FileUtils.copyInputStreamToFile(inputStream, temp);

        return temp;
    }

    protected void waitForIdleSync() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    protected void openDrawer(IntentsTestRule activityRule) {
        Activity sut = activityRule.launchActivity(null);

        shortSleep();

        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());

        waitForIdleSync();

        Screenshot.snapActivity(sut).record();
    }

    protected Activity getCurrentActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry
                .getInstance()
                .getActivitiesInStage(Stage.RESUMED);

            if (resumedActivities.iterator().hasNext()) {
                currentActivity = resumedActivities.iterator().next();
            }
        });

        return currentActivity;
    }

    protected void shortSleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void longSleep() {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public OCFile createFolder(String remotePath) {
        TestCase.assertTrue(new CreateFolderOperation(remotePath, user, targetContext)
                                .execute(client, getStorageManager())
                                .isSuccess());

        return getStorageManager().getFileByDecryptedRemotePath(remotePath);
    }

    public void uploadOCUpload(OCUpload ocUpload) {
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
            FileUploader.LOCAL_BEHAVIOUR_COPY,
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
    }

    protected void screenshot(Activity sut) {
        Screenshot.snapActivity(sut).record();
    }
}
