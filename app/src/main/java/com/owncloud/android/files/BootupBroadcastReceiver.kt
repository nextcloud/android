/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.di.ApplicationScope
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.WalledCheckCache
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class BootupBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var powerManagementService: PowerManagementService

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var walledCheckCache: WalledCheckCache

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Log_OC.d(TAG, "Getting wrong intent: ${intent.action}")
            return
        }

        initSyncOperations(context)
        scheduleAutoUploadJobs()
        MainApp.initContactsBackup(arbitraryDataProvider, accountManager, backgroundJobManager)
    }

    private fun initSyncOperations(context: Context) {
        MainApp.initSyncOperations(
            context,
            preferences,
            uploadsStorageManager,
            accountManager,
            connectivityService,
            powerManagementService,
            backgroundJobManager,
            clock,
            walledCheckCache,
            appScope
        )
    }

    private fun scheduleAutoUploadJobs() {
        Log_OC.d(TAG, "scheduleContentObserverJob, called")
        backgroundJobManager.scheduleContentObserverJob()
        backgroundJobManager.schedulePeriodicAutoUpload()
    }

    companion object {
        private val TAG: String = BootupBroadcastReceiver::class.java.simpleName
    }
}
