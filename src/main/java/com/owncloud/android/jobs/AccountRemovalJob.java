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

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.FilesSyncHelper;

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

public class AccountRemovalJob extends Job implements AccountManagerCallback<Boolean> {
    public static final String TAG = "AccountRemovalJob";
    public static final String ACCOUNT = "account";

    private UploadsStorageManager uploadsStorageManager;
    private UserAccountManager accountManager;

    public AccountRemovalJob(UploadsStorageManager uploadStorageManager, UserAccountManager accountManager) {
        this.uploadsStorageManager = uploadStorageManager;
        this.accountManager = accountManager;
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Context context = MainApp.getAppContext();
        PersistableBundleCompat bundle = params.getExtras();
        Account account = accountManager.getAccountByName(bundle.getString(ACCOUNT, ""));
        AccountManager am = (AccountManager) context.getSystemService(ACCOUNT_SERVICE);

        if (account != null && am != null) {
            // disable contact backup job
            ContactsPreferenceActivity.cancelContactBackupJobForAccount(context, account);

            am.removeAccount(account, this, null);

            FileDataStorageManager storageManager = new FileDataStorageManager(account, context.getContentResolver());

            File tempDir = new File(FileStorageUtils.getTemporalPath(account.name));
            File saveDir = new File(FileStorageUtils.getSavePath(account.name));

            FileStorageUtils.deleteRecursively(tempDir, storageManager);
            FileStorageUtils.deleteRecursively(saveDir, storageManager);

            // delete all database entries
            storageManager.deleteAllFiles();

            // remove pending account removal
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());
            arbitraryDataProvider.deleteKeyForAccount(account.name, PENDING_FOR_REMOVAL);

            // remove synced folders set for account
            SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(context.getContentResolver(),
                                                                                 AppPreferencesImpl.fromContext(context));
            List<SyncedFolder> syncedFolders = syncedFolderProvider.getSyncedFolders();

            List<Long> syncedFolderIds = new ArrayList<>();

            for (SyncedFolder syncedFolder : syncedFolders) {
                if (syncedFolder.getAccount().equals(account.name)) {
                    arbitraryDataProvider.deleteKeyForAccount(FilesSyncHelper.GLOBAL,
                            FilesSyncHelper.SYNCEDFOLDERINITIATED + syncedFolder.getId());
                    syncedFolderIds.add(syncedFolder.getId());
                }
            }

            syncedFolderProvider.deleteSyncFoldersForAccount(account);

            uploadsStorageManager.removeAccountUploads(account);

            FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(context.getContentResolver());

            for (long syncedFolderId : syncedFolderIds) {
                filesystemDataProvider.deleteAllEntriesForSyncedFolder(Long.toString(syncedFolderId));
            }

            // delete stored E2E keys
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY);
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY);

            return Result.SUCCESS;
        } else {
            return Result.FAILURE;
        }
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            EventBus.getDefault().post(new AccountRemovedEvent());
        }
    }
}
