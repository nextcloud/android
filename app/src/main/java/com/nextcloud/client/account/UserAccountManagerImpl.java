/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.extensions.AccountExtensionsKt;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserAccountManagerImpl implements UserAccountManager {

    private static final String TAG = UserAccountManagerImpl.class.getSimpleName();
    private static final String PREF_SELECT_OC_ACCOUNT = "select_oc_account";

    private Context context;
    private final AccountManager accountManager;

    public static UserAccountManagerImpl fromContext(Context context) {
        AccountManager am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        return new UserAccountManagerImpl(context, am);
    }

    @Inject
    public UserAccountManagerImpl(
        Context context,
        AccountManager accountManager
    ) {
        this.context = context;
        this.accountManager = accountManager;
    }

    @Override
    public void removeAllAccounts() {
        for (Account account : getAccounts()) {
            accountManager.removeAccount(account, null, null);
        }
    }

    @Override
    public boolean removeUser(User user) {
        try {
            AccountManagerFuture<Boolean> result = accountManager.removeAccount(user.toPlatformAccount(),
                                                                                null,
                                                                                null);
            return result.getResult();
        } catch (OperationCanceledException| AuthenticatorException| IOException ex) {
            return false;
        }
    }

    @Override
    @NonNull
    public Account[] getAccounts() {
        return accountManager.getAccountsByType(getAccountType());
    }

    @Override
    @NonNull
    public List<User> getAllUsers() {
        Account[] accounts = getAccounts();
        List<User> users = new ArrayList<>(accounts.length);
        for (Account account : accounts) {
            User user = createUserFromAccount(account);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    @Override
    public boolean exists(Account account) {
        Account[] nextcloudAccounts = getAccounts();

        if (account != null && account.name != null) {
            int lastAtPos = account.name.lastIndexOf('@');
            String hostAndPort = account.name.substring(lastAtPos + 1);
            String username = account.name.substring(0, lastAtPos);
            String otherHostAndPort;
            String otherUsername;
            for (Account otherAccount : nextcloudAccounts) {
                lastAtPos = otherAccount.name.lastIndexOf('@');
                otherHostAndPort = otherAccount.name.substring(lastAtPos + 1);
                otherUsername = otherAccount.name.substring(0, lastAtPos);
                if (otherHostAndPort.equals(hostAndPort) &&
                    otherUsername.equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @NonNull
    public Account getCurrentAccount() {
        Account[] ocAccounts = getAccounts();

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountName = appPreferences.getString(PREF_SELECT_OC_ACCOUNT, null);

        Account defaultAccount = Arrays.stream(ocAccounts)
            .filter(account -> account.name.equals(accountName))
            .findFirst()
            .orElse(null);

        // take first which is not pending for removal account as fallback
        if (defaultAccount == null) {
            defaultAccount = Arrays.stream(ocAccounts)
                .filter(account -> !arbitraryDataProvider.getBooleanValue(account.name, PENDING_FOR_REMOVAL))
                .findFirst()
                .orElse(null);
        }

        if (defaultAccount == null) {
            if (ocAccounts.length > 0) {
                defaultAccount = ocAccounts[0];
            } else {
                defaultAccount = getAnonymousAccount();
            }
        }

        return defaultAccount;
    }

    private Account getAnonymousAccount() {
        return new Account("Anonymous", context.getString(R.string.anonymous_account_type));
    }

    /**
     * Temporary solution to convert platform account to user instance.
     * It takes null and returns null on error to ease error handling
     * in legacy code.
     *
     * @param account Account instance
     * @return User instance or null, if conversion failed
     */
    @Nullable
    private User createUserFromAccount(@NonNull Account account) {
        if (AccountExtensionsKt.isAnonymous(account, context)) {
            return null;
        }

        if (context == null) {
            Log_OC.d(TAG, "Context is null MainApp.getAppContext() used");
            context = MainApp.getAppContext();
        }

        OwnCloudAccount ownCloudAccount;
        try {
            ownCloudAccount = new OwnCloudAccount(account, context);
        } catch (AccountUtils.AccountNotFoundException ex) {
            return null;
        }

        /*
         * Server version
         */
        String serverVersionStr = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_VERSION);
        OwnCloudVersion serverVersion;
        if (serverVersionStr != null) {
            serverVersion = new OwnCloudVersion(serverVersionStr);
        } else {
            serverVersion = MainApp.MINIMUM_SUPPORTED_SERVER_VERSION;
        }

        /*
         * Server address
         */
        String serverAddressStr = accountManager.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL);
        if (serverAddressStr == null || serverAddressStr.isEmpty()) {
            return AnonymousUser.fromContext(context);
        }
        URI serverUri = URI.create(serverAddressStr); // TODO: validate

        return new RegisteredUser(
            account,
            ownCloudAccount,
            new Server(serverUri, serverVersion)
        );
    }

    /**
     * Get user. If user cannot be retrieved due to data error, anonymous user is returned instead.
     *
     *
     * @return User instance
     */
    @NonNull
    @Override
    public User getUser() {
        Account account = getCurrentAccount();
        User user = createUserFromAccount(account);
        return user != null ? user : AnonymousUser.fromContext(context);
    }

    @Override
    @NonNull
    public Optional<User> getUser(CharSequence accountName) {
        Account account = getAccountByName(accountName.toString());
        User user = createUserFromAccount(account);
        return Optional.ofNullable(user);
    }

    @Override
    public User getAnonymousUser() {
        return AnonymousUser.fromContext(context);
    }

    @Override
    @Nullable
    public OwnCloudAccount getCurrentOwnCloudAccount() {
        try {
            Account currentPlatformAccount = getCurrentAccount();
            return new OwnCloudAccount(currentPlatformAccount, context);
        } catch (AccountUtils.AccountNotFoundException | IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    @NonNull
    public Account getAccountByName(String name) {
        for (Account account : getAccounts()) {
            if (account.name.equals(name)) {
                return account;
            }
        }

        return getAnonymousAccount();
    }

    @Override
    public boolean setCurrentOwnCloudAccount(String accountName) {
        boolean result = false;
        if (accountName != null) {
            for (final Account account : getAccounts()) {
                if (accountName.equals(account.name)) {
                    SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    appPrefs.putString(PREF_SELECT_OC_ACCOUNT, accountName);
                    appPrefs.apply();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean setCurrentOwnCloudAccount(int hashCode) {
        boolean result = false;
        if (hashCode != 0) {
            for (final User user : getAllUsers()) {
                if (hashCode == user.hashCode()) {
                    SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    appPrefs.putString(PREF_SELECT_OC_ACCOUNT, user.getAccountName());
                    appPrefs.apply();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Deprecated
    @Override
    @NonNull
    public OwnCloudVersion getServerVersion(Account account) {
        OwnCloudVersion serverVersion = MainApp.MINIMUM_SUPPORTED_SERVER_VERSION;

        if (account != null) {
            AccountManager accountMgr = AccountManager.get(MainApp.getAppContext());
            String serverVersionStr = accountMgr.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION);

            if (serverVersionStr != null) {
                serverVersion = new OwnCloudVersion(serverVersionStr);
            }
        }

        return serverVersion;
    }

    @Override
    public void resetOwnCloudAccount() {
        SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        appPrefs.putString(PREF_SELECT_OC_ACCOUNT, null);
        appPrefs.apply();
    }

    @Override
    public  boolean accountOwnsFile(OCFile file, Account account) {
        final String ownerId = file.getOwnerId();
        return TextUtils.isEmpty(ownerId) || account.name.split("@")[0].equalsIgnoreCase(ownerId);
    }

    @Override
    public boolean userOwnsFile(OCFile file, User user) {
        return accountOwnsFile(file, user.toPlatformAccount());
    }

    public boolean migrateUserId() {
        Account[] ocAccounts = accountManager.getAccountsByType(MainApp.getAccountType(context));
        String userId;
        String displayName;
        GetUserInfoRemoteOperation remoteUserNameOperation = new GetUserInfoRemoteOperation();
        int failed = 0;
        for (Account account : ocAccounts) {
            String storedUserId = accountManager.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

            if (!TextUtils.isEmpty(storedUserId)) {
                continue;
            }

            // add userId
            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                NextcloudClient nextcloudClient = OwnCloudClientManagerFactory
                    .getDefaultSingleton()
                    .getNextcloudClientFor(ocAccount, context);

                RemoteOperationResult<UserInfo> result = remoteUserNameOperation.execute(nextcloudClient);

                if (result.isSuccess()) {
                    UserInfo userInfo = result.getResultData();
                    userId = userInfo.getId();
                    displayName = userInfo.getDisplayName();
                } else {
                    // skip account, try it next time
                    Log_OC.e(TAG, "Error while getting username for account: " + account.name);
                    failed++;
                    continue;
                }
            } catch (Exception e) {
                Log_OC.e(TAG, "Error while getting username: " + e.getMessage());
                failed++;
                continue;
            }

            accountManager.setUserData(account,
                                       com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_DISPLAY_NAME,
                                       displayName);
            accountManager.setUserData(account,
                                       com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID,
                                       userId);
        }

        return failed == 0;
    }

    private String getAccountType() {
        return context.getString(R.string.account_type);
    }

    @Override
    public void startAccountCreation(final Activity activity) {
        Intent intent = new Intent(context, AuthenticatorActivity.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
