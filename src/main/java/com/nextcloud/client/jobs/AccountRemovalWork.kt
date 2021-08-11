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
package com.nextcloud.client.jobs

import android.accounts.Account
import android.content.Context
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.common.NextcloudClient
import com.nextcloud.java.util.Optional
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.FilesystemDataProvider
import com.owncloud.android.datamodel.PushConfigurationState
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.DeleteAppPasswordRemoteOperation
import com.owncloud.android.lib.resources.users.RemoteWipeSuccessRemoteOperation
import com.owncloud.android.providers.DocumentsStorageProvider
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.activity.ManageAccountsActivity
import com.owncloud.android.ui.events.AccountRemovedEvent
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.PushUtils
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.ArrayList

/**
 * Removes account and all local files
 */
@Suppress("LongParameterList") // legacy code
class AccountRemovalWork(
    private val context: Context,
    params: WorkerParameters,
    private val uploadsStorageManager: UploadsStorageManager,
    private val userAccountManager: UserAccountManager,
    private val backgroundJobManager: BackgroundJobManager,
    private val clock: Clock,
    private val eventBus: EventBus
) : Worker(context, params) {

    companion object {
        const val TAG = "AccountRemovalJob"
        const val ACCOUNT = "account"
        const val REMOTE_WIPE = "remote_wipe"
    }

    @Suppress("ReturnCount") // legacy code
    override fun doWork(): Result {
        val accountName = inputData.getString(ACCOUNT) ?: ""
        if (TextUtils.isEmpty(accountName)) { // didn't receive account to delete
            return Result.failure()
        }
        val optionalUser = userAccountManager.getUser(accountName)
        if (!optionalUser.isPresent) { // trying to delete non-existing user
            return Result.failure()
        }
        val remoteWipe = inputData.getBoolean(REMOTE_WIPE, false)
        val arbitraryDataProvider = ArbitraryDataProvider(context.contentResolver)
        val user = optionalUser.get()
        backgroundJobManager.cancelPeriodicContactsBackup(user)
        val userRemoved = userAccountManager.removeUser(user)
        val storageManager = FileDataStorageManager(user.toPlatformAccount(), context.contentResolver)

        // disable daily backup
        arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
            "false"
        )
        // unregister push notifications
        unregisterPushNotifications(context, user, arbitraryDataProvider)

        // remove pending account removal
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, ManageAccountsActivity.PENDING_FOR_REMOVAL)

        // remove synced folders set for account
        remoceSyncedFolders(context, user.toPlatformAccount(), clock)

        // delete all uploads for account
        uploadsStorageManager.removeUserUploads(user)

        // delete stored E2E keys and mnemonic
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, EncryptionUtils.PUBLIC_KEY)
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, EncryptionUtils.MNEMONIC)

        // remove all files
        removeFiles(user, storageManager)
        // delete all database entries
        storageManager.deleteAllFiles()

        if (remoteWipe) {
            val optionalClient = createClient(user)
            if (optionalClient.isPresent) {
                val client = optionalClient.get()
                val authToken = client.credentials.authToken
                RemoteWipeSuccessRemoteOperation(authToken).execute(client)
            }
        }
        // notify Document Provider
        DocumentsStorageProvider.notifyRootsChanged(context)

        // delete app password
        val deleteAppPasswordRemoteOperation = DeleteAppPasswordRemoteOperation()
        val optionNextcloudClient = createNextcloudClient(user)

        if (optionNextcloudClient.isPresent) {
            deleteAppPasswordRemoteOperation.execute(optionNextcloudClient.get())
        }

        if (userRemoved) {
            eventBus.post(AccountRemovedEvent())
        }

        return Result.success()
    }

    private fun unregisterPushNotifications(
        context: Context,
        user: User,
        arbitraryDataProvider: ArbitraryDataProvider
    ) {
        val arbitraryDataPushString = arbitraryDataProvider.getValue(user, PushUtils.KEY_PUSH)
        val pushServerUrl = context.resources.getString(R.string.push_server_url)
        if (!TextUtils.isEmpty(arbitraryDataPushString) && !TextUtils.isEmpty(pushServerUrl)) {
            val gson = Gson()
            val pushArbitraryData = gson.fromJson(
                arbitraryDataPushString,
                PushConfigurationState::class.java
            )
            pushArbitraryData.isShouldBeDeleted = true
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                PushUtils.KEY_PUSH,
                gson.toJson(pushArbitraryData)
            )
            PushUtils.pushRegistrationToServer(userAccountManager, pushArbitraryData.getPushToken())
        }
    }

    private fun remoceSyncedFolders(context: Context, account: Account, clock: Clock) {
        val syncedFolderProvider = SyncedFolderProvider(
            context.contentResolver,
            AppPreferencesImpl.fromContext(context),
            clock
        )
        val syncedFolders = syncedFolderProvider.syncedFolders
        val syncedFolderIds: MutableList<Long> = ArrayList()
        for (syncedFolder in syncedFolders) {
            if (syncedFolder.account == account.name) {
                syncedFolderIds.add(syncedFolder.id)
            }
        }
        syncedFolderProvider.deleteSyncFoldersForAccount(account)
        val filesystemDataProvider = FilesystemDataProvider(context.contentResolver)
        for (syncedFolderId in syncedFolderIds) {
            filesystemDataProvider.deleteAllEntriesForSyncedFolder(java.lang.Long.toString(syncedFolderId))
        }
    }

    private fun removeFiles(user: User, storageManager: FileDataStorageManager) {
        val tempDir = File(FileStorageUtils.getTemporalPath(user.accountName))
        val saveDir = File(FileStorageUtils.getSavePath(user.accountName))
        FileStorageUtils.deleteRecursively(tempDir, storageManager)
        FileStorageUtils.deleteRecursively(saveDir, storageManager)
    }

    private fun createClient(user: User): Optional<OwnCloudClient> {
        @Suppress("TooGenericExceptionCaught") // needs migration to newer api to get rid of exceptions
        return try {
            val context = MainApp.getAppContext()
            val factory = OwnCloudClientManagerFactory.getDefaultSingleton()
            val client = factory.getClientFor(user.toOwnCloudAccount(), context)
            Optional.of(client)
        } catch (e: Exception) {
            Log_OC.e(this, "Could not create client", e)
            Optional.empty()
        }
    }

    private fun createNextcloudClient(user: User): Optional<NextcloudClient> {
        @Suppress("TooGenericExceptionCaught") // needs migration to newer api to get rid of exceptions
        return try {
            val context = MainApp.getAppContext()
            val factory = OwnCloudClientManagerFactory.getDefaultSingleton()
            val client = factory.getNextcloudClientFor(user.toOwnCloudAccount(), context)
            Optional.of(client)
        } catch (e: Exception) {
            Log_OC.e(this, "Could not create client", e)
            Optional.empty()
        }
    }
}
