package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;

/**
 * Absolute base class holding the oC account.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = BaseActivity.class.getSimpleName();

    /**
     * ownCloud {@link Account} where the main {@link OCFile} handled by the activity is located.
     */
    private Account mCurrentAccount;

    /**
     * Capabilites of the server where {@link #mCurrentAccount} lives.
     */
    private OCCapability mCapabilities;

    /**
     * Flag to signal that the activity will is finishing to enforce the creation of an ownCloud {@link Account}.
     */
    private boolean mRedirectingToSetupAccount = false;

    /**
     * Flag to signal when the value of mAccount was set.
     */
    protected boolean mAccountWasSet;

    /**
     * Flag to signal when the value of mAccount was restored from a saved state.
     */
    protected boolean mAccountWasRestored;

    /**
     * Access point to the cached database for the current ownCloud {@link Account}.
     */
    private FileDataStorageManager mStorageManager = null;

    /**
     * Sets and validates the ownCloud {@link Account} associated to the Activity.
     * <p/>
     * If not valid, tries to swap it for other valid and existing ownCloud {@link Account}.
     * <p/>
     * POSTCONDITION: updates {@link #mAccountWasSet} and {@link #mAccountWasRestored}.
     *
     * @param account      New {@link Account} to set.
     * @param savedAccount When 'true', account was retrieved from a saved instance state.
     */
    protected void setAccount(Account account, boolean savedAccount) {
        Account oldAccount = mCurrentAccount;
        boolean validAccount =
                (account != null && AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(),
                        account.name));
        if (validAccount) {
            mCurrentAccount = account;
            mAccountWasSet = true;
            mAccountWasRestored = (savedAccount || mCurrentAccount.equals(oldAccount));

        } else {
            swapToDefaultAccount();
        }
    }

    /**
     * Tries to swap the current ownCloud {@link Account} for other valid and existing.
     * <p/>
     * If no valid ownCloud {@link Account} exists, the the user is requested
     * to create a new ownCloud {@link Account}.
     * <p/>
     * POSTCONDITION: updates {@link #mAccountWasSet} and {@link #mAccountWasRestored}.
     */
    protected void swapToDefaultAccount() {
        // default to the most recently used account
        Account newAccount = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
        if (newAccount == null) {
            /// no account available: force account creation
            createAccount();
            mRedirectingToSetupAccount = true;
            mAccountWasSet = false;
            mAccountWasRestored = false;

        } else {
            mAccountWasSet = true;
            mAccountWasRestored = (newAccount.equals(mCurrentAccount));
            mCurrentAccount = newAccount;
        }
    }

    /**
     * Launches the account creation activity. To use when no ownCloud account is available.
     */
    protected void createAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(),
                null,
                null,
                null,
                this,
                new AccountCreationCallback(),
                new Handler());
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     * <p/>
     * Child classes must grant that state depending on the {@link Account} is updated.
     */
    protected void onAccountSet(boolean stateWasRecovered) {
        if (getAccount() != null) {
            mStorageManager = new FileDataStorageManager(getAccount(), getContentResolver());
            mCapabilities = mStorageManager.getCapability(mCurrentAccount.name);
        } else {
            Log_OC.wtf(TAG, "onAccountChanged was called with NULL account associated!");
        }
    }

    protected void setAccount(Account account) {
        mCurrentAccount = account;
    }

    /**
     * Getter for the capabilities of the server where the current OC account lives.
     *
     * @return Capabilities of the server where the current OC account lives. Null if the account is not
     * set yet.
     */
    public OCCapability getCapabilities() {
        return mCapabilities;
    }

    /**
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     *
     * @return OwnCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     */
    public Account getAccount() {
        return mCurrentAccount;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mAccountWasSet) {
            onAccountSet(mAccountWasRestored);
        }
    }

    /**
     * @return 'True' when the Activity is finishing to enforce the setup of a new account.
     */
    protected boolean isRedirectingToSetupAccount() {
        return mRedirectingToSetupAccount;
    }

    public FileDataStorageManager getStorageManager() {
        return mStorageManager;
    }

    /**
     * Method that gets called when a new account has been successfully created.
     *
     * @param future
     */
    protected void onAccountCreationSuccessful(AccountManagerFuture<Bundle> future) {
        // no special handling in base activity
    }

    /**
     * Helper class handling a callback from the {@link AccountManager} after the creation of
     * a new ownCloud {@link Account} finished, successfully or not.
     */
    public class AccountCreationCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            BaseActivity.this.mRedirectingToSetupAccount = false;
            boolean accountWasSet = false;
            if (future != null) {
                try {
                    Bundle result;
                    result = future.getResult();
                    String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String type = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    if (AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name)) {
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
            if (!accountWasSet) {
                moveTaskToBack(true);
            }
        }
    }
}
