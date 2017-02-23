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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.adapter.AccountListAdapter;
import com.owncloud.android.ui.adapter.AccountListItem;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.Set;

/**
 * An Activity that allows the user to manage accounts.
 */
public class ManageAccountsActivity extends FileActivity
        implements AccountListAdapter.AccountListAdapterListener, AccountManagerCallback<Boolean>, ComponentsGetter {
    private static final String TAG = ManageAccountsActivity.class.getSimpleName();
    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED";

    private ListView mListView;
    private final Handler mHandler = new Handler();
    private String mAccountName;
    private AccountListAdapter mAccountListAdapter;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;
    Set<String> mOriginalAccounts;
    String mOriginalCurrentAccount;
    private Drawable mTintedCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTintedCheck = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.ic_account_circle_white_18dp));
        int tint = ContextCompat.getColor(this, R.color.primary);
        DrawableCompat.setTint(mTintedCheck, tint);

        setContentView(R.layout.accounts_layout);

        mListView = (ListView) findViewById(R.id.account_list);

        setupToolbar();
        updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.prefs_manage_accounts));

        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        mOriginalAccounts = DisplayUtils.toAccountNameSet(accountList);
        mOriginalCurrentAccount = AccountUtils.getCurrentOwnCloudAccount(this).name;

        setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
        onAccountSet(false);

        mAccountListAdapter = new AccountListAdapter(this, getAccountListItems(), mTintedCheck);

        mListView.setAdapter(mAccountListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchAccount(mAccountListAdapter.getItem(position).getAccount());
            }
        });

        initializeComponentGetters();
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged());
        resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged());
        setResult(RESULT_OK, resultIntent);
        
        super.onBackPressed();
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return <code>true</code> if aacount list has changed, <code>false</code> if not
     */
    private boolean hasAccountListChanged() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        Set<String> actualAccounts = DisplayUtils.toAccountNameSet(accountList);
        return !mOriginalAccounts.equals(actualAccounts);
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return <code>true</code> if aacount list has changed, <code>false</code> if not
     */
    private boolean hasCurrentAccountChanged() {
        Account account = AccountUtils.getCurrentOwnCloudAccount(this);
        if (account == null) {
            return true;
        } else {
            return !mOriginalCurrentAccount.equals(account.name);
        }
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
        ArrayList<AccountListItem> adapterAccountList = new ArrayList<>(accountList.length);
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
    public void performAccountRemoval(Account account) {
        AccountRemovalConfirmationDialog dialog = AccountRemovalConfirmationDialog.newInstance(account);
        mAccountName = account.name;
        dialog.show(getFragmentManager(), "dialog");
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
                                mAccountListAdapter = new AccountListAdapter(
                                    ManageAccountsActivity.this,
                                    getAccountListItems(),
                                    mTintedCheck
                                );
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

    public void switchAccount(Account account) {
        if (getAccount().name.equals(account.name)) {
            // current account selected, just go back
            finish();
        } else {
            // restart list of files with new account
            AccountUtils.setCurrentOwnCloudAccount(ManageAccountsActivity.this, account.name);
            Intent i = new Intent(ManageAccountsActivity.this, FileDisplayActivity.class);
            i.putExtra(FileActivity.EXTRA_ACCOUNT, account);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
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
            
            if (AccountUtils.getCurrentOwnCloudAccount(this) == null) {
                String accountName = "";
                Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
                if (accounts.length != 0) {
                    accountName = accounts[0].name;
                }
                AccountUtils.setCurrentOwnCloudAccount(this, accountName);
            }

            mAccountListAdapter = new AccountListAdapter(this, getAccountListItems(), mTintedCheck);
            mListView.setAdapter(mAccountListAdapter);
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

    public Handler getHandler() { return mHandler; }

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
        return super.getStorageManager();
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

    public static class AccountRemovalConfirmationDialog extends DialogFragment {

        private static final String ARG__ACCOUNT = "account";

        private Account mAccount;

        public static AccountRemovalConfirmationDialog newInstance(Account account) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(ARG__ACCOUNT, account);

            AccountRemovalConfirmationDialog dialog = new AccountRemovalConfirmationDialog();
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mAccount = getArguments().getParcelable(ARG__ACCOUNT);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.delete_account)
                    .setMessage(getResources().getString(R.string.delete_account_warning, mAccount.name))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.common_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    AccountManager am = (AccountManager) getActivity().getSystemService(ACCOUNT_SERVICE);
                                    am.removeAccount(
                                            mAccount,
                                            (ManageAccountsActivity)getActivity(),
                                            ((ManageAccountsActivity)getActivity()).getHandler());
                                }
                            })
                    .setNegativeButton(R.string.common_cancel, null)
                    .create();
        }
    }
}
