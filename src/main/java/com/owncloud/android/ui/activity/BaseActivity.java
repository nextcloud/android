package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.DarkMode;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManagerImpl;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity with common behaviour for activities dealing with ownCloud {@link Account}s .
 */
public abstract class BaseActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = BaseActivity.class.getSimpleName();

    /**
     * ownCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     */
    private Account currentAccount;

    /**
     * Capabilities of the server where {@link #currentAccount} lives.
     */
    private OCCapability capabilities;

    /**
     * Access point to the cached database for the current ownCloud {@link Account}.
     */
    private FileDataStorageManager storageManager;

    /**
     * Tracks whether the activity should be recreate()'d after a theme change
     */
    private boolean themeChangePending;
    private boolean paused;
    protected boolean enableAccountHandling = true;

    @Inject UserAccountManager accountManager;
    @Inject AppPreferences preferences;

    private AppPreferences.Listener onPreferencesChanged = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            onThemeSettingsModeChanged();
        }
    };

    public UserAccountManager getUserAccountManager() {
        return accountManager;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (enableAccountHandling) {
            Account account = accountManager.getCurrentAccount();
            setAccount(account, false);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        preferences.addListener(onPreferencesChanged);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        preferences.removeListener(onPreferencesChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        if (themeChangePending) {
            recreate();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log_OC.v(TAG, "onNewIntent() start");
        Account current = accountManager.getCurrentAccount();
        if (current != null && currentAccount != null && !currentAccount.name.equals(current.name)) {
            currentAccount = current;
        }
        Log_OC.v(TAG, "onNewIntent() stop");
    }

    /**
     *  Since ownCloud {@link Account}s can be managed from the system setting menu, the existence of the {@link
     *  Account} associated to the instance must be checked every time it is restarted.
     */
    @Override
    protected void onRestart() {
        Log_OC.v(TAG, "onRestart() start");
        super.onRestart();
        boolean validAccount = currentAccount != null && accountManager.exists(currentAccount);
        if (!validAccount) {
            swapToDefaultAccount();
        }
        Log_OC.v(TAG, "onRestart() end");
    }

    private void onThemeSettingsModeChanged() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
    }

    /**
     * Sets and validates the ownCloud {@link Account} associated to the Activity.
     *
     * If not valid, tries to swap it for other valid and existing ownCloud {@link Account}.
     *
     * @param account      New {@link Account} to set.
     * @param savedAccount When 'true', account was retrieved from a saved instance state.
     */
    @Deprecated
    protected void setAccount(Account account, boolean savedAccount) {
        boolean validAccount = account != null && accountManager.setCurrentOwnCloudAccount(account.name);
        if (validAccount) {
            currentAccount = account;
        } else {
            swapToDefaultAccount();
        }

        if(currentAccount != null) {
            storageManager = new FileDataStorageManagerImpl(currentAccount, this);
            capabilities = storageManager.getCapability(currentAccount.name);
        }
    }

    protected void setUser(User user) {
        setAccount(user.toPlatformAccount(), false);
    }

    /**
     * Tries to swap the current ownCloud {@link Account} for other valid and existing.
     *
     * If no valid ownCloud {@link Account} exists, then the user is requested
     * to create a new ownCloud {@link Account}.
     */
    protected void swapToDefaultAccount() {
        // default to the most recently used account
        Account newAccount = accountManager.getCurrentAccount();

        if (newAccount == null) {
            /// no account available: force account creation
            createAccount(true);

            if (enableAccountHandling) {
                finish();
            }
        } else {
            currentAccount = newAccount;
        }
    }

    /**
     * Launches the account creation activity.
     *
     * @param mandatoryCreation     When 'true', if an account is not created by the user, the app will be closed.
     *                              To use when no ownCloud account is available.
     */
    protected void createAccount(boolean mandatoryCreation) {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(this),
                null,
                null,
                null,
                this,
                new AccountCreationCallback(mandatoryCreation),
                new Handler());
    }

    /**
     * Getter for the capabilities of the server where the current OC account lives.
     *
     * @return Capabilities of the server where the current OC account lives. Null if the account is not
     * set yet.
     */
    public OCCapability getCapabilities() {
        return capabilities;
    }

    /**
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     *
     * @return OwnCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     */
    public Account getAccount() {
        return currentAccount;
    }

    public Optional<User> getUser() {
        if (currentAccount != null) {
            return accountManager.getUser(currentAccount.name);
        } else {
            return Optional.empty();
        }
    }

    public FileDataStorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * Method that gets called when a new account has been successfully created.
     *
     * @param future
     */
    protected void onAccountCreationSuccessful(AccountManagerFuture<Bundle> future) {
        // no special handling in base activity
        Log_OC.d(TAG,"onAccountCreationSuccessful");
    }

    /**
     * Helper class handling a callback from the {@link AccountManager} after the creation of
     * a new ownCloud {@link Account} finished, successfully or not.
     */
    public class AccountCreationCallback implements AccountManagerCallback<Bundle> {

        boolean mMandatoryCreation;

        /**
         * Constructor
         *
         * @param mandatoryCreation     When 'true', if an account was not created, the app is closed.
         */
        public AccountCreationCallback(boolean mandatoryCreation) {
            mMandatoryCreation = mandatoryCreation;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            boolean accountWasSet = false;
            if (future != null) {
                try {
                    Bundle result;
                    result = future.getResult();
                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    if (accountManager.setCurrentOwnCloudAccount(name)) {
                        setAccount(new Account(name, type), false);
                        accountWasSet = true;
                    }

                    onAccountCreationSuccessful(future);
                } catch (OperationCanceledException e) {
                    Log_OC.d(TAG, "Account creation canceled");

                } catch (Exception e) {
                    Log_OC.e(TAG, "Account creation finished in exception: ", e);
                }

            } else {
                Log_OC.e(TAG, "Account creation callback with null bundle");
            }
            if (mMandatoryCreation && !accountWasSet) {
                moveTaskToBack(true);
            }
        }
    }
}
