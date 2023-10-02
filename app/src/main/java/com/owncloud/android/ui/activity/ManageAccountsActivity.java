/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz  <hello@ezaquarii.com>
 * @author Chawki Chouib  <chouibc@gmail.com>
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Chawki Chouib  <chouibc@gmail.com>
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
import android.view.View;

import com.google.common.collect.Sets;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.adapter.UserListItem;
import com.owncloud.android.ui.dialog.AccountRemovalDialog;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.owncloud.android.ui.activity.UserInfoActivity.KEY_USER_DATA;
import static com.owncloud.android.ui.adapter.UserListAdapter.KEY_DISPLAY_NAME;
import static com.owncloud.android.ui.adapter.UserListAdapter.KEY_USER_INFO_REQUEST_CODE;

/**
 * An Activity that allows the user to manage accounts.
 */
public class ManageAccountsActivity extends FileActivity implements UserListAdapter.Listener,
    AccountManagerCallback<Boolean>,
    ComponentsGetter,
    UserListAdapter.ClickListener {
    private static final String TAG = ManageAccountsActivity.class.getSimpleName();

    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED";
    public static final String PENDING_FOR_REMOVAL = UserAccountManager.PENDING_FOR_REMOVAL;

    private static final int KEY_DELETE_CODE = 101;
    private static final int SINGLE_ACCOUNT = 1;
    private static final int MIN_MULTI_ACCOUNT_SIZE = 2;

    private RecyclerView recyclerView;
    private final Handler handler = new Handler();
    private String accountName;
    private UserListAdapter userListAdapter;
    private ServiceConnection downloadServiceConnection;
    private ServiceConnection uploadServiceConnection;
    private Set<String> originalUsers;
    private String originalCurrentUser;

    private ArbitraryDataProvider arbitraryDataProvider;
    private boolean multipleAccountsSupported;

    @Inject BackgroundJobManager backgroundJobManager;
    @Inject UserAccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accounts_layout);

        recyclerView = findViewById(R.id.account_list);

        setupToolbar();

        // set the back button from action bar
        ActionBar actionBar = getSupportActionBar();

        // check if is not null
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            viewThemeUtils.files.themeActionBar(this, actionBar, R.string.prefs_manage_accounts);
        }

        List<User> users = accountManager.getAllUsers();
        originalUsers = toAccountNames(users);

        Optional<User> currentUser = getUser();
        if (currentUser.isPresent()) {
            originalCurrentUser = currentUser.get().getAccountName();
        }

        arbitraryDataProvider = new ArbitraryDataProviderImpl(this);

        multipleAccountsSupported = getResources().getBoolean(R.bool.multiaccount_support);

        userListAdapter = new UserListAdapter(this,
                                              accountManager,
                                              getUserListItems(),
                                              this,
                                              multipleAccountsSupported,
                                              true,
                                              true,
                                              viewThemeUtils);

        recyclerView.setAdapter(userListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initializeComponentGetters();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == KEY_DELETE_CODE && data != null) {
            Bundle bundle = data.getExtras();
            if (bundle != null && bundle.containsKey(UserInfoActivity.KEY_ACCOUNT)) {
                final Account account = bundle.getParcelable(UserInfoActivity.KEY_ACCOUNT);
                if (account != null) {
                    User user = accountManager.getUser(account.name).orElseThrow(RuntimeException::new);
                    accountName = account.name;
                    performAccountRemoval(user);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        if (accountManager.getAllUsers().size() > 0) {
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged());
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged());
            setResult(RESULT_OK, resultIntent);

            super.onBackPressed();
        } else {
            final Intent intent = new Intent(this, AuthenticatorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return true if account list has changed, false if not
     */
    private boolean hasAccountListChanged() {
        List<User> users = accountManager.getAllUsers();
        List<User> newList = new ArrayList<>();
        for (User user : users) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(user, PENDING_FOR_REMOVAL);

            if (!pendingForRemoval) {
                newList.add(user);
            }
        }
        Set<String> actualAccounts = toAccountNames(newList);
        return !originalUsers.equals(actualAccounts);
    }

    private static Set<String> toAccountNames(Collection<User> users) {
        Set<String> accountNames = Sets.newHashSetWithExpectedSize(users.size());
        for (User user : users) {
            accountNames.add(user.getAccountName());
        }
        return accountNames;
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return true if account list has changed, false if not
     */
    private boolean hasCurrentAccountChanged() {
        User user = getUserAccountManager().getUser();
        if (user.isAnonymous()) {
            return true;
        } else {
            return !user.getAccountName().equals(originalCurrentUser);
        }
    }

    /**
     * Initialize ComponentsGetters.
     */
    private void initializeComponentGetters() {
        downloadServiceConnection = newTransferenceServiceConnection();
        if (downloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), downloadServiceConnection,
                        Context.BIND_AUTO_CREATE);
        }
        uploadServiceConnection = newTransferenceServiceConnection();
        if (uploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), uploadServiceConnection,
                        Context.BIND_AUTO_CREATE);
        }
    }

    private List<UserListItem> getUserListItems() {
        List<User> users = accountManager.getAllUsers();
        List<UserListItem> userListItems = new ArrayList<>(users.size());
        for (User user : users) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(user, PENDING_FOR_REMOVAL);
            userListItems.add(new UserListItem(user, !pendingForRemoval));
        }

        if (getResources().getBoolean(R.bool.multiaccount_support)) {
            userListItems.add(new UserListItem());
        }

        return userListItems;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else {
            retval = super.onOptionsItemSelected(item);
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
    public void startAccountCreation() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.getAccountType(this),
                      null,
                      null,
                      null,
                      this,
                      future -> {
                          if (future != null) {
                              try {
                                  Bundle result = future.getResult();
                                  String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                                  accountManager.setCurrentOwnCloudAccount(name);
                                  userListAdapter = new UserListAdapter(
                                      this,
                                      accountManager,
                                      getUserListItems(),
                                      this,
                                      multipleAccountsSupported,
                                      false,
                                      true,
                                      viewThemeUtils);
                                  recyclerView.setAdapter(userListAdapter);
                                  runOnUiThread(() -> userListAdapter.notifyDataSetChanged());
                              } catch (OperationCanceledException e) {
                                  Log_OC.d(TAG, "Account creation canceled");
                              } catch (Exception e) {
                                  Log_OC.e(TAG, "Account creation finished in exception: ", e);
                              }
                          }
                      }, handler);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountRemovedEvent(AccountRemovedEvent event) {
        List<UserListItem> userListItemArray = getUserListItems();
        userListAdapter.clear();
        userListAdapter.addAll(userListItemArray);
        userListAdapter.notifyDataSetChanged();
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            // after remove account
            Optional<User> user = accountManager.getUser(accountName);
            if (!user.isPresent()) {
                // Cancel transfers of the removed account
                if (mUploaderBinder != null) {
                    mUploaderBinder.cancel(accountName);
                }
                if (mDownloaderBinder != null) {
                    mDownloaderBinder.cancel(accountName);
                }
            }

            User currentUser = getUserAccountManager().getUser();
            if (currentUser.isAnonymous()) {
                String accountName = "";
                List<User> users = accountManager.getAllUsers();
                if (users.size() > 0) {
                    accountName = users.get(0).getAccountName();
                }
                accountManager.setCurrentOwnCloudAccount(accountName);
            }

            List<UserListItem> userListItemArray = getUserListItems();
            if (userListItemArray.size() > SINGLE_ACCOUNT) {
                userListAdapter = new UserListAdapter(this,
                                                      accountManager,
                                                      userListItemArray,
                                                      this,
                                                      multipleAccountsSupported,
                                                      false,
                                                      true,
                                                      viewThemeUtils);
                recyclerView.setAdapter(userListAdapter);
            } else {
                onBackPressed();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (downloadServiceConnection != null) {
            unbindService(downloadServiceConnection);
            downloadServiceConnection = null;
        }
        if (uploadServiceConnection != null) {
            unbindService(uploadServiceConnection);
            uploadServiceConnection = null;
        }

        super.onDestroy();
    }

    public Handler getHandler() {
        return handler;
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
        return super.getStorageManager();
    }

    @Override
    public FileOperationsHelper getFileOperationsHelper() {
        return null;
    }

    protected ServiceConnection newTransferenceServiceConnection() {
        return new ManageAccountsServiceConnection();
    }

    private void performAccountRemoval(User user) {
        // disable account in recycler view
        for (int i = 0; i < userListAdapter.getItemCount(); i++) {
            UserListItem item = userListAdapter.getItem(i);

            if (item != null && item.getUser().getAccountName().equalsIgnoreCase(user.getAccountName())) {
                item.setEnabled(false);
                break;
            }

            userListAdapter.notifyDataSetChanged();
        }

        // store pending account removal
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(this);
        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), PENDING_FOR_REMOVAL, String.valueOf(true));

        // Cancel transfers
        if (mUploaderBinder != null) {
            mUploaderBinder.cancel(user);
        }
        if (mDownloaderBinder != null) {
            mDownloaderBinder.cancel(user.getAccountName());
        }

        backgroundJobManager.startAccountRemovalJob(user.getAccountName(), false);

        // immediately select a new account
        List<User> users = accountManager.getAllUsers();

        String newAccountName = "";
        for (User u : users) {
            if (!u.getAccountName().equalsIgnoreCase(u.getAccountName())) {
                newAccountName = u.getAccountName();
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
        if (users.size() < MIN_MULTI_ACCOUNT_SIZE) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, true);
            resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, true);
            setResult(RESULT_OK, resultIntent);

            super.onBackPressed();
        }
    }

    public static void openAccountRemovalDialog(User user, FragmentManager fragmentManager) {
        AccountRemovalDialog dialog = AccountRemovalDialog.newInstance(user);
        dialog.show(fragmentManager, "dialog");
    }

    private void openAccount(User user) {
        final Intent intent = new Intent(this, UserInfoActivity.class);
        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, user);
        OwnCloudAccount oca = user.toOwnCloudAccount();
        intent.putExtra(KEY_DISPLAY_NAME, oca.getDisplayName());
        startActivityForResult(intent, KEY_USER_INFO_REQUEST_CODE);
    }

    @VisibleForTesting
    public void showUser(User user, UserInfo userInfo) {
        final Intent intent = new Intent(this, UserInfoActivity.class);
        OwnCloudAccount oca = user.toOwnCloudAccount();
        intent.putExtra(UserInfoActivity.KEY_ACCOUNT, user);
        intent.putExtra(KEY_DISPLAY_NAME, oca.getDisplayName());
        intent.putExtra(KEY_USER_DATA, userInfo);
        startActivityForResult(intent, KEY_USER_INFO_REQUEST_CODE);
    }

    @Override
    public void onOptionItemClicked(User user, View view) {
        if (view.getId() == R.id.account_menu) {
            PopupMenu popup = new PopupMenu(this, view);
            popup.getMenuInflater().inflate(R.menu.item_account, popup.getMenu());

            if (accountManager.getUser().equals(user)) {
                popup.getMenu().findItem(R.id.action_open_account).setVisible(false);
            }
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.action_open_account) {
                    accountClicked(user.hashCode());
                } else if (itemId == R.id.action_delete_account) {
                    openAccountRemovalDialog(user, getSupportFragmentManager());
                } else {
                    openAccount(user);
                }

                return true;
            });
            popup.show();
        } else {
            openAccount(user);
        }
    }

    @Override
    public void onAccountClicked(User user) {
        openAccount(user);
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
