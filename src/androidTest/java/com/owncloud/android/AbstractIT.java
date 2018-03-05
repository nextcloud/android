package com.owncloud.android;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ApplicationTestCase;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

/**
 * Created by tobi on 3/2/18.
 */

@RunWith(AndroidJUnit4.class)
public abstract class AbstractIT extends ApplicationTestCase<MainApp> {

    protected static OwnCloudClient client;
    protected static Account account;
    protected static Context context;

    private static final String username = "tobi";
    private static final String password = "tobi";
    private static final String baseUrl = "server/nc";

    public AbstractIT() {
        super(MainApp.class);
    }

    @Rule // todo only working with rules 1.0.2-alpha2
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @BeforeClass
    public static void beforeAll() {
        try {
            context = MainApp.getAppContext();

            Account temp = new Account(username + "@" + baseUrl, MainApp.getAccountType());

            if (!com.owncloud.android.authentication.AccountUtils.exists(temp, context)) {
                AccountManager accountManager = AccountManager.get(context);
                accountManager.addAccountExplicitly(temp, password, null);
                accountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                        Integer.toString(com.owncloud.android.authentication.AccountUtils.ACCOUNT_VERSION));
                accountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_VERSION, "14.0.0.0");
                accountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "http://" + baseUrl);
            }

            account = com.owncloud.android.authentication.AccountUtils.getOwnCloudAccountByName(context, username + "@" + baseUrl);
            client = OwnCloudClientFactory.createOwnCloudClient(account, context);

            createDummyFiles();
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
        return new FileDataStorageManager(account, context.getContentResolver());
    }

    private static void createDummyFiles() throws IOException {
        new File(FileStorageUtils.getSavePath(account.name)).mkdirs();

        File file = new File(FileStorageUtils.getSavePath(account.name) + "/123.txt");
        file.createNewFile();
    }
}
