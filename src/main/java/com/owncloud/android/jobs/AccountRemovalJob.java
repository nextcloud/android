/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManager;
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

import static android.content.Context.ACCOUNT_SERVICE;
import static com.owncloud.android.ui.activity.ManageAccountsActivity.PENDING_FOR_REMOVAL;

/**
 * Removes account and all local files
 */
public class AccountRemovalJob extends Job {
    public static final String TAG = "AccountRemovalJob";
    public static final String ACCOUNT = "account";
    public static final String REMOTE_WIPE = "remote_wipe";

    private final UploadsStorageManager uploadsStorageManager;
    private final UserAccountManager userAccountManager;
    private final BackgroundJobManager backgroundJobManager;
    private final Clock clock;
    private final EventBus eventBus;

    public AccountRemovalJob(UploadsStorageManager uploadStorageManager,
                             UserAccountManager accountManager,
                             BackgroundJobManager backgroundJobManager,
                             Clock clock,
                             EventBus eventBus) {
        this.uploadsStorageManager = uploadStorageManager;
        this.userAccountManager = accountManager;
        this.backgroundJobManager = backgroundJobManager;
        this.clock = clock;
        this.eventBus = eventBus;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Context context = MainApp.getAppContext();
        PersistableBundleCompat bundle = params.getExtras();
        String accountName = bundle.getString(ACCOUNT, "");
        if (TextUtils.isEmpty(accountName)) {
            // didn't receive account to delete
            return Result.FAILURE;
        }
        Optional<User> optionalUser = userAccountManager.getUser(accountName);
        if (!optionalUser.isPresent()) {
            // trying to delete non-existing user
            return Result.FAILURE;
        }
        AccountManager accountManager = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);
        if (accountManager == null) {
            return Result.FAILURE;
        }
        boolean remoteWipe = bundle.getBoolean(REMOTE_WIPE, false);

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());

        User user = optionalUser.get();
        backgroundJobManager.cancelPeriodicContactsBackup(user);

        final boolean userRemoved = userAccountManager.removeUser(user);
        if (userRemoved) {
            eventBus.post(new AccountRemovedEvent());
        }

        FileDataStorageManager storageManager = new FileDataStorageManager(user.toPlatformAccount(), context.getContentResolver());

        // remove all files
        removeFiles(user, storageManager);

        // delete all database entries
        storageManager.deleteAllFiles();

        // disable daily backup
        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                    ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                                                    "false");

        // unregister push notifications
        unregisterPushNotifications(context, user, arbitraryDataProvider);

        // remove pending account removal
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), PENDING_FOR_REMOVAL);

        // remove synced folders set for account
        remoceSyncedFolders(context, user.toPlatformAccount(), clock);

        // delete all uploads for account
        uploadsStorageManager.removeAccountUploads(user.toPlatformAccount());

        // delete stored E2E keys
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        if (remoteWipe) {
            Optional<OwnCloudClient> optionalClient = createClient(user);
            if (optionalClient.isPresent()) {
                OwnCloudClient client = optionalClient.get();
                String authToken = client.getCredentials().getAuthToken();
                new RemoteWipeSuccessRemoteOperation(authToken).execute(client);
            }
        }

        // notify Document Provider
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String authority = context.getResources().getString(R.string.document_provider_authority);
            Uri rootsUri = DocumentsContract.buildRootsUri(authority);
            context.getContentResolver().notifyChange(rootsUri, null);
        }

        return Result.SUCCESS;
    }

    private void unregisterPushNotifications(Context context,
                                             User user,
                                             ArbitraryDataProvider arbitraryDataProvider) {
        final String arbitraryDataPushString = arbitraryDataProvider.getValue(user.toPlatformAccount(),
                                                                              PushUtils.KEY_PUSH);
        final String pushServerUrl = context.getResources().getString(R.string.push_server_url);
        if (!TextUtils.isEmpty(arbitraryDataPushString) && !TextUtils.isEmpty(pushServerUrl)) {
            Gson gson = new Gson();
            PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryDataPushString,
                                                                     PushConfigurationState.class);
            pushArbitraryData.setShouldBeDeleted(true);
            arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), PushUtils.KEY_PUSH,
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

    private void removeFiles(User user, FileDataStorageManager storageManager) {
        File tempDir = new File(FileStorageUtils.getTemporalPath(user.getAccountName()));
        File saveDir = new File(FileStorageUtils.getSavePath(user.getAccountName()));

        FileStorageUtils.deleteRecursively(tempDir, storageManager);
        FileStorageUtils.deleteRecursively(saveDir, storageManager);
    }

    private Optional<OwnCloudClient> createClient(User user) {
        try {
            Context context = MainApp.getAppContext();
            OwnCloudClientManager factory = OwnCloudClientManagerFactory.getDefaultSingleton();
            OwnCloudClient client = factory.getClientFor(user.toOwnCloudAccount(), context);
            return Optional.of(client);
        } catch (Exception e) {
            Log_OC.e(this, "Could not create client", e);
            return Optional.empty();
        }
    }
}
