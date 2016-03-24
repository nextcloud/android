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
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

/**
 * Managing the accounts.
 */
public class ManageAccountsActivity extends ToolbarActivity
        implements AccountListAdapter.AccountListAdapterListener, AccountManagerCallback<Boolean>, ComponentsGetter {
    private static final String TAG = ManageAccountsActivity.class.getSimpleName();

    private ListView mListView;
    private final Handler mHandler = new Handler();
    private String mAccountName;
    private AccountListAdapter mAccountListAdapter;
    protected FileUploader.FileUploaderBinder mUploaderBinder = null;
    protected FileDownloader.FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accounts_layout);

        mListView = (ListView) findViewById(R.id.account_list);

        setupToolbar();
        updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.prefs_manage_accounts));

        mAccountListAdapter = new AccountListAdapter(this, getAccountListItems());

        mListView.setAdapter(mAccountListAdapter);

        initializeComponentGetters();
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
    @NonNull
    private ArrayList<AccountListItem> getAccountListItems() {
        AccountManager am = (AccountManager) this.getSystemService(this.ACCOUNT_SERVICE);
        Account[] accountList = am.getAccountsByType(MainApp.getAccountType());
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
                Account[] accounts = AccountManager.get(this)
                        .getAccountsByType(MainApp.getAccountType());
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
            } else {
                return;
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

    ;
}
