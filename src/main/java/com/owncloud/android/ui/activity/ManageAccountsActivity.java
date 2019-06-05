/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.jobs.AccountRemovalJob;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.adapter.AccountListAdapter;
import com.owncloud.android.ui.adapter.AccountListItem;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

/**
 * An Activity that allows the user to manage accounts.
 */
public class ManageAccountsActivity extends FileActivity
        implements AccountListAdapter.AccountListAdapterListener, AccountManagerCallback<Boolean>, ComponentsGetter {
    private static final String TAG = ManageAccountsActivity.class.getSimpleName();

    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED";
    public static final String PENDING_FOR_REMOVAL = "PENDING_FOR_REMOVAL";

    private static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";

    private static final int KEY_USER_INFO_REQUEST_CODE = 13;
    private static final int KEY_DELETE_CODE = 101;
    private static final int SINGLE_ACCOUNT = 1;
    private static final int MIN_MULTI_ACCOUNT_SIZE = 2;

    private ListView mListView;
    private final Handler mHandler = new Handler();
    private String mAccountName;
    private AccountListAdapter mAccountListAdapter;
    private ServiceConnection mDownloadServiceConnection;
    private ServiceConnection mUploadServiceConnection;
    private Set<String> mOriginalAccounts;
    private String mOriginalCurrentAccount;
    private Drawable mTintedCheck;

    private ArbitraryDataProvider arbitraryDataProvider;

    @Inject UserAccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTintedCheck = DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.account_circle_white));
        int tint = ThemeUtils.elementColor(this);
        DrawableCompat.setTint(mTintedCheck, tint);

        setContentView(R.layout.accounts_layout);

        mListView = findViewById(R.id.account_list);

        setupToolbar();
        updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.prefs_manage_accounts));

        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));
        mOriginalAccounts = DisplayUtils.toAccountNameSet(Arrays.asList(accountList));

        Account currentAccount = getUserAccountManager().getCurrentAccount();

        if (currentAccount != null) {
            mOriginalCurrentAccount = currentAccount.name;
        }

        setAccount(currentAccount);
        onAccountSet(false);

        arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());

        mAccountListAdapter = new AccountListAdapter(this, getUserAccountManager(), getAccountListItems(), mTintedCheck);

        mListView.setAdapter(mAccountListAdapter);

        final Intent intent = new Intent(this, UserInfoActivity.class);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AccountListItem item = mAccountListAdapter.getItem(position);

                if (item != null && item.isEnabled()) {
                    Account account = item.getAccount();
                    intent.putExtra(UserInfoActivity.KEY_ACCOUNT, Parcels.wrap(account));
                    try {
                        OwnCloudAccount oca = new OwnCloudAccount(account, MainApp.getAppContext());
                        intent.putExtra(KEY_DISPLAY_NAME, oca.getDisplayName());
                    } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                        Log_OC.d(TAG, "Failed to find NC account");
                    }

                    startActivityForResult(intent, KEY_USER_INFO_REQUEST_CODE);
                }
            }
        });

        initializeComponentGetters();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case KEY_DELETE_CODE:
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    if (bundle.containsKey(UserInfoActivity.KEY_ACCOUNT)) {
                        Account account = Parcels.unwrap(bundle.getParcelable(UserInfoActivity.KEY_ACCOUNT));
                        mAccountName = account.name;
                        performAccountRemoval(account);
                    }
                }
                break;
            default:
                break;
        }
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
     * @return true if account list has changed, false if not
     */
    private boolean hasAccountListChanged() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));

        ArrayList<Account> newList = new ArrayList<>();
        for (Account account : accountList) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(account, PENDING_FOR_REMOVAL);

            if (!pendingForRemoval) {
                newList.add(account);
            }
        }

        Set<String> actualAccounts = DisplayUtils.toAccountNameSet(newList);
        return !mOriginalAccounts.equals(actualAccounts);
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return true if account list has changed, false if not
     */
    private boolean hasCurrentAccountChanged() {
        Account account = getUserAccountManager().getCurrentAccount();
        if (account == null) {
            return true;
        } else {
            return !account.name.equals(mOriginalCurrentAccount);
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
    private List<AccountListItem> getAccountListItems() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));
        List<AccountListItem> adapterAccountList = new ArrayList<>(accountList.length);
        for (Account account : accountList) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(account, PENDING_FOR_REMOVAL);
            adapterAccountList.add(new AccountListItem(account, !pendingForRemoval));
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
                break;
        }
        return retval;
    }

    @Override
    public void showFirstRunActivity() {
        Intent firstRunIntent = new Intent(getApplicationContext(), FirstRunActivity.class);
        firstRunIntent.putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true);
        startActivity(firstRunIntent);
    }

    @Override
    public void createAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(this),
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
                                accountManager.setCurrentOwnCloudAccount(name);
                                mAccountListAdapter = new AccountListAdapter(
                                        ManageAccountsActivity.this,
                                        getUserAccountManager(),
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountRemovedEvent(AccountRemovedEvent event) {
        List<AccountListItem> accountListItemArray = getAccountListItems();
        mAccountListAdapter.clear();
        mAccountListAdapter.addAll(accountListItemArray);
        mAccountListAdapter.notifyDataSetChanged();
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            // after remove account
            Account account = new Account(mAccountName, MainApp.getAccountType(this));
            if (!accountManager.exists(account)) {
                // Cancel transfers of the removed account
                if (mUploaderBinder != null) {
                    mUploaderBinder.cancel(account);
                }
                if (mDownloaderBinder != null) {
                    mDownloaderBinder.cancel(account);
                }
            }

            if (getUserAccountManager().getCurrentAccount() == null) {
                String accountName = "";
                Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));
                if (accounts.length != 0) {
                    accountName = accounts[0].name;
                }
                accountManager.setCurrentOwnCloudAccount(accountName);
            }

            List<AccountListItem> accountListItemArray = getAccountListItems();
            if (accountListItemArray.size() > SINGLE_ACCOUNT) {
                mAccountListAdapter = new AccountListAdapter(this, getUserAccountManager(), accountListItemArray, mTintedCheck);
                mListView.setAdapter(mAccountListAdapter);
            } else {
                onBackPressed();
            }
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

    private void performAccountRemoval(Account account) {
        // disable account in list view
        for (int i = 0; i < mAccountListAdapter.getCount(); i++) {
            AccountListItem item = mAccountListAdapter.getItem(i);

            if (item != null && item.getAccount().equals(account)) {
                item.setEnabled(false);
                break;
            }

            mAccountListAdapter.notifyDataSetChanged();
        }

        // store pending account removal
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
        arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PENDING_FOR_REMOVAL, String.valueOf(true));

        // Cancel transfers
        if (mUploaderBinder != null) {
            mUploaderBinder.cancel(account);
        }
        if (mDownloaderBinder != null) {
            mDownloaderBinder.cancel(account);
        }

        // schedule job
        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(AccountRemovalJob.ACCOUNT, account.name);

        new JobRequest.Builder(AccountRemovalJob.TAG)
                .startNow()
                .setExtras(bundle)
                .setUpdateCurrent(false)
                .build()
                .schedule();

        // immediately select a new account
        Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));

        String newAccountName = "";
        for (Account acc: accounts) {
            if (!account.name.equalsIgnoreCase(acc.name)) {
                newAccountName = acc.name;
                break;
            }
        }

        if (newAccountName.isEmpty()) {
            Log_OC.d(TAG, "new account set to null");
            accountManager.resetOwnCloudAccount();
        } else {
            Log_OC.d(TAG, "new account set to: " + newAccountName);
            accountManager.setCurrentOwnCloudAccount(newAccountName);
        }

        // only one to be (deleted) account remaining
        if (accounts.length < MIN_MULTI_ACCOUNT_SIZE) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, true);
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, true);
            setResult(RESULT_OK, resultIntent);

            super.onBackPressed();
        }
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
