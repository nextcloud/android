/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.ListView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.files.FileOperationsHelper;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.adapter.AccountListAdapter;
import com.owncloud.android.ui.adapter.AccountListItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * An Activity that allows the user to manage accounts.
 */
public class ManageAccountsActivity extends ToolbarActivity
        implements AccountListAdapter.AccountListAdapterListener, AccountManagerCallback<Boolean>, ComponentsGetter {
    private static final String TAG = ManageAccountsActivity.class.getSimpleName();
    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED";

    private ListView mListView;
    private final Handler mHandler = new Handler();
    private String mAccountName;
    private AccountListAdapter mAccountListAdapter;
    protected FileUploader.FileUploaderBinder mUploaderBinder = null;
    protected FileDownloader.FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;
    Set<String> mOriginalAccounts;
    String mOriginalCurrentAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accounts_layout);

        mListView = (ListView) findViewById(R.id.account_list);

        setupToolbar();
        updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.prefs_manage_accounts));

        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        mOriginalAccounts = toAccountNameSet(accountList);
        mOriginalCurrentAccount = AccountUtils.getCurrentOwnCloudAccount(this).name;

        mAccountListAdapter = new AccountListAdapter(this, getAccountListItems());

        mListView.setAdapter(mAccountListAdapter);

        initializeComponentGetters();
    }

    /**
     * converts an array of accounts into a set of account names.
     *
     * @param accountList the account array
     * @return set of account names
     */
    private Set<String> toAccountNameSet(Account[] accountList) {
        Set<String> actualAccounts = new HashSet<String>(accountList.length);
        for (Account account : accountList) {
            actualAccounts.add(account.name);
        }
        return actualAccounts;
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged());
        resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged());
        setResult(RESULT_OK, resultIntent);

        finish();
        super.onBackPressed();
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return <code>true</code> if aacount list has changed, <code>false</code> if not
     */
    private boolean hasAccountListChanged() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        Set<String> actualAccounts = toAccountNameSet(accountList);
        return !mOriginalAccounts.equals(actualAccounts);
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return <code>true</code> if aacount list has changed, <code>false</code> if not
     */
    private boolean hasCurrentAccountChanged() {
        String currentAccount = AccountUtils.getCurrentOwnCloudAccount(this).name;
        return !mOriginalCurrentAccount.equals(currentAccount);
    }

    /**
     * Initialize ComponentsGetters.
     */
    private void initializeComponentGetters() {
        mDownloadServiceConnection = newTransferenceServiceConnection();
        if (mDownloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), mDownloadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
        mUploadServiceConnection = newTransferenceServiceConnection();
        if (mUploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), mUploadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * creates the account list items list including the add-account action in case multiaccount_support is enabled.
     *
     * @return list of account list items
     */
    private ArrayList<AccountListItem> getAccountListItems() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        ArrayList<AccountListItem> adapterAccountList = new ArrayList<AccountListItem>(accountList.length);
        for (Account account : accountList) {
            adapterAccountList.add(new AccountListItem(account));
        }

        // Add Create Account item at the end of account list if multi-account is enabled
        if (getResources().getBoolean(R.bool.multiaccount_support)) {
            adapterAccountList.add(new AccountListItem());
        }

        return adapterAccountList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public void removeAccount(Account account) {
        AccountManager am = (AccountManager) this.getSystemService(ACCOUNT_SERVICE);
        mAccountName = account.name;
        am.removeAccount(account, ManageAccountsActivity.this, mHandler);
        Log_OC.d(TAG, "Remove an account " + account.name);
    }

    @Override
    public void changePasswordOfAccount(Account account) {
        Intent updateAccountCredentials = new Intent(ManageAccountsActivity.this, AuthenticatorActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACCOUNT, account);
        updateAccountCredentials.putExtra(AuthenticatorActivity.EXTRA_ACTION,
                AuthenticatorActivity.ACTION_UPDATE_TOKEN);
        startActivity(updateAccountCredentials);
    }

    @Override
    public void createAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(),
                null,
                null,
                null,
                this,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        if (future != null) {
                            try {
                                Bundle result = future.getResult();
                                String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                                AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name);
                                mAccountListAdapter = new AccountListAdapter(ManageAccountsActivity
                                        .this, getAccountListItems());
                                mListView.setAdapter(mAccountListAdapter);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAccountListAdapter.notifyDataSetChanged();
                                    }
                                });
                            } catch (OperationCanceledException e) {
                                Log_OC.d(TAG, "Account creation canceled");
                            } catch (Exception e) {
                                Log_OC.e(TAG, "Account creation finished in exception: ", e);
                            }
                        }
                    }
                }, mHandler);
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            // after remove account
            Account account = new Account(mAccountName, MainApp.getAccountType());
            if (!AccountUtils.exists(account, MainApp.getAppContext())) {
                // Cancel transfers of the removed account
                if (mUploaderBinder != null) {
                    mUploaderBinder.cancel(account);
                }
                if (mDownloaderBinder != null) {
                    mDownloaderBinder.cancel(account);
                }
            }

            Account a = AccountUtils.getCurrentOwnCloudAccount(this);
            String accountName = "";
            if (a == null) {
                Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
                if (accounts.length != 0)
                    accountName = accounts[0].name;
                AccountUtils.setCurrentOwnCloudAccount(this, accountName);
            }

            mAccountListAdapter = new AccountListAdapter(this, getAccountListItems());
            mAccountListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        if (mDownloadServiceConnection != null) {
            unbindService(mDownloadServiceConnection);
            mDownloadServiceConnection = null;
        }
        if (mUploadServiceConnection != null) {
            unbindService(mUploadServiceConnection);
            mUploadServiceConnection = null;
        }

        super.onDestroy();
    }

    // Methods for ComponentsGetter
    @Override
    public FileDownloader.FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }

    @Override
    public FileUploader.FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }

    @Override
    public OperationsService.OperationsServiceBinder getOperationsServiceBinder() {
        return null;
    }

    @Override
    public FileDataStorageManager getStorageManager() {
        return null;
    }

    @Override
    public FileOperationsHelper getFileOperationsHelper() {
        return null;
    }

    protected ServiceConnection newTransferenceServiceConnection() {
        return new ManageAccountsServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class ManageAccountsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {

            if (component.equals(new ComponentName(ManageAccountsActivity.this, FileDownloader.class))) {
                mDownloaderBinder = (FileDownloader.FileDownloaderBinder) service;

            } else if (component.equals(new ComponentName(ManageAccountsActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploader.FileUploaderBinder) service;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(ManageAccountsActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(ManageAccountsActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    }
}
