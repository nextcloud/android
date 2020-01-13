/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.RemoteWipeSuccessRemoteOperation;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PushUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.content.Context.ACCOUNT_SERVICE;
import static com.owncloud.android.ui.activity.ManageAccountsActivity.PENDING_FOR_REMOVAL;

/**
 * Removes account and all local files
 */
public class AccountRemovalJob extends Job implements AccountManagerCallback<Boolean> {
    public static final String TAG = "AccountRemovalJob";
    public static final String ACCOUNT = "account";
    public static final String REMOTE_WIPE = "remote_wipe";

    private final UploadsStorageManager uploadsStorageManager;
    private final UserAccountManager userAccountManager;
    private final Clock clock;

    public AccountRemovalJob(UploadsStorageManager uploadStorageManager, UserAccountManager accountManager, Clock clock) {
        this.uploadsStorageManager = uploadStorageManager;
        this.userAccountManager = accountManager;
        this.clock = clock;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Context context = MainApp.getAppContext();
        PersistableBundleCompat bundle = params.getExtras();
        Account account = userAccountManager.getAccountByName(bundle.getString(ACCOUNT, ""));
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        boolean remoteWipe = bundle.getBoolean(REMOTE_WIPE, false);

        if (account != null && accountManager != null) {
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());

            // disable contact backup job
            ContactsPreferenceActivity.cancelContactBackupJobForAccount(context, account);

            removeAccount(account, accountManager);

            FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());

            // remove all files
            removeFiles(account, storageManager);

            // delete all database entries
            storageManager.deleteAllFiles();

            // remove contact backup job
            ContactsPreferenceActivity.cancelContactBackupJobForAccount(context, account);

            // disable daily backup
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                                                        "false");

            // unregister push notifications
            unregisterPushNotifications(context, account, arbitraryDataProvider);

            // remove pending account removal
            arbitraryDataProvider.deleteKeyForAccount(account.name, PENDING_FOR_REMOVAL);

            // remove synced folders set for account
            remoceSyncedFolders(context, account, clock);

            // delete all uploads for account
            uploadsStorageManager.removeAccountUploads(account);

            // delete stored E2E keys
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY);
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY);

            OwnCloudClient client = createClient(account);
            if (remoteWipe && client != null) {
                String authToken = client.getCredentials().getAuthToken();
                new RemoteWipeSuccessRemoteOperation(authToken).execute(client);
            }

            // notify Document Provider
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String authority = context.getResources().getString(R.string.document_provider_authority);
                Uri rootsUri = DocumentsContract.buildRootsUri(authority);
                context.getContentResolver().notifyChange(rootsUri, null);
            }

            return Result.SUCCESS;
        } else {
            return Result.FAILURE;
        }
    }

    private void unregisterPushNotifications(Context context, Account account, ArbitraryDataProvider arbitraryDataProvider) {
        String arbitraryDataPushString;

        if (!TextUtils.isEmpty(arbitraryDataPushString = arbitraryDataProvider.getValue(
            account, PushUtils.KEY_PUSH)) &&
            !TextUtils.isEmpty(context.getResources().getString(R.string.push_server_url))) {
            Gson gson = new Gson();
            PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryDataPushString,
                                                                     PushConfigurationState.class);
            pushArbitraryData.setShouldBeDeleted(true);
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PushUtils.KEY_PUSH,
                                                        gson.toJson(pushArbitraryData));

            PushUtils.pushRegistrationToServer(userAccountManager, pushArbitraryData.getPushToken());
        }
    }

    private void remoceSyncedFolders(Context context, Account account, Clock clock) {
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(context.getContentResolver(),
                                                                             AppPreferencesImpl.fromContext(context),
                                                                             clock);
        List<SyncedFolder> syncedFolders = syncedFolderProvider.getSyncedFolders();

        List<Long> syncedFolderIds = new ArrayList<>();

        for (SyncedFolder syncedFolder : syncedFolders) {
            if (syncedFolder.getAccount().equals(account.name)) {
                syncedFolderIds.add(syncedFolder.getId());
            }
        }

        syncedFolderProvider.deleteSyncFoldersForAccount(account);

        FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(context.getContentResolver());

        for (long syncedFolderId : syncedFolderIds) {
            filesystemDataProvider.deleteAllEntriesForSyncedFolder(Long.toString(syncedFolderId));
        }
    }

    private void removeFiles(Account account, FileDataStorageManager storageManager) {
        File tempDir = new File(FileStorageUtils.getTemporalPath(account.name));
        File saveDir = new File(FileStorageUtils.getSavePath(account.name));

        FileStorageUtils.deleteRecursively(tempDir, storageManager);
        FileStorageUtils.deleteRecursively(saveDir, storageManager);
    }

    private void removeAccount(Account account, AccountManager accountManager) {
        try {
            AccountManagerFuture<Boolean> accountRemoval = accountManager.removeAccount(account, this, null);
            boolean removal = accountRemoval.getResult();

            if (!removal) {
                Log_OC.e(this, "Account removal of " + account.name + " failed!");
            }
        } catch (Exception e) {
            Log_OC.e(this, "Account removal of " + account.name + " failed!", e);
        }
    }

    @Nullable
    private OwnCloudClient createClient(Account account) {
        OwnCloudClient client = null;
        try {
            OwnCloudAccount ocAccount = new OwnCloudAccount(account, MainApp.getAppContext());
            client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount,
                                                                                     MainApp.getAppContext());
        } catch (Exception e) {
            Log_OC.e(this, "Could not create client", e);
        }
        return client;
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            EventBus.getDefault().post(new AccountRemovedEvent());
        }
    }
}
