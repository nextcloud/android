package com.owncloud.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.testing.screenshot.Screenshot;
import com.nextcloud.client.RetryTestRule;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.utils.FileStorageUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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

//@RunWith(AndroidJUnit4.class)
public abstract class AbstractIT {
    @Rule public RetryTestRule retryTestRule = new RetryTestRule();

    protected static OwnCloudClient client;
    protected static Account account;
    protected static Context targetContext;

    private Activity currentActivity;

    @BeforeClass
    public static void beforeAll() {
        try {
            targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Bundle arguments = androidx.test.platform.app.InstrumentationRegistry.getArguments();

            Uri baseUrl = Uri.parse(arguments.getString("TEST_SERVER_URL"));
            String loginName = arguments.getString("TEST_SERVER_USERNAME");
            String password = arguments.getString("TEST_SERVER_PASSWORD");

            Account temp = new Account(loginName + "@" + baseUrl, MainApp.getAccountType(targetContext));
            UserAccountManager accountManager = UserAccountManagerImpl.fromContext(targetContext);
            if (!accountManager.exists(temp)) {
                AccountManager platformAccountManager = AccountManager.get(targetContext);
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

            client = OwnCloudClientFactory.createOwnCloudClient(account, targetContext);

            createDummyFiles();

            waitForServer(client, baseUrl);
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
        RemoteOperationResult result = new ReadFolderRemoteOperation("/").execute(client);
        assertTrue(result.getLogMessage(), result.isSuccess());

        for (Object object : result.getData()) {
            RemoteFile remoteFile = (RemoteFile) object;

            if (!remoteFile.getRemotePath().equals("/")) {
                assertTrue(new RemoveFileRemoteOperation(remoteFile.getRemotePath())
                               .execute(client).isSuccess());
            }
        }
    }


    protected FileDataStorageManager getStorageManager() {
        return new FileDataStorageManager(account, targetContext.getContentResolver());
    }

    private static void createDummyFiles() throws IOException {
        new File(FileStorageUtils.getSavePath(account.name)).mkdirs();

        createFile("empty.txt", 0);
        createFile("nonEmpty.txt", 100);
        createFile("chunkedFile.txt", 500000);
    }

    private static void createFile(String name, int iteration) throws IOException {
        File file = new File(FileStorageUtils.getSavePath(account.name) + File.separator + name);
        file.createNewFile();

        FileWriter writer = new FileWriter(file);

        for (int i = 0; i < iteration; i++) {
            writer.write("123123123123123123123123123\n");
        }
        writer.flush();
        writer.close();
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

    protected File getFile(String filename) throws IOException {
        InputStream inputStream = getInstrumentation().getContext().getAssets().open(filename);
        File temp = File.createTempFile("file", "file");
        FileUtils.copyInputStreamToFile(inputStream, temp);

        return temp;
    }

    protected void waitForIdleSync() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    protected void openDrawer(IntentsTestRule activityRule) throws InterruptedException {
        Activity sut = activityRule.launchActivity(null);

        Thread.sleep(3000);

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
}
