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
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.utils.Log_OC
import dagger.android.AndroidInjection
import javax.inject.Inject

class BootupBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            Log_OC.d(TAG, "Getting wrong intent: ${intent.action}")
            return
        }

        scheduleAutoUploadJobs()
        MainApp.initContactsBackup(arbitraryDataProvider, accountManager, backgroundJobManager)
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
