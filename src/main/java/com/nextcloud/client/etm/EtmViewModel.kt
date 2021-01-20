/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.etm

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.etm.pages.EtmAccountsFragment
import com.nextcloud.client.etm.pages.EtmBackgroundJobsFragment
import com.nextcloud.client.etm.pages.EtmFileTransferFragment
import com.nextcloud.client.etm.pages.EtmMigrations
import com.nextcloud.client.etm.pages.EtmPreferencesFragment
import com.nextcloud.client.files.downloader.TransferManagerConnection
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.JobInfo
import com.nextcloud.client.migrations.MigrationInfo
import com.nextcloud.client.migrations.MigrationsDb
import com.nextcloud.client.migrations.MigrationsManager
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountUtils
import javax.inject.Inject

@Suppress("LongParameterList") // Dependencies Injection
@SuppressLint("StaticFieldLeak")
class EtmViewModel @Inject constructor(
    private val context: Context,
    private val defaultPreferences: SharedPreferences,
    private val platformAccountManager: AccountManager,
    private val accountManager: UserAccountManager,
    private val resources: Resources,
    private val backgroundJobManager: BackgroundJobManager,
    private val migrationsManager: MigrationsManager,
    private val migrationsDb: MigrationsDb
) : ViewModel() {

    companion object {
        val ACCOUNT_USER_DATA_KEYS = listOf(
            // AccountUtils.Constants.KEY_COOKIES, is disabled
            AccountUtils.Constants.KEY_DISPLAY_NAME,
            AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
            AccountUtils.Constants.KEY_OC_BASE_URL,
            AccountUtils.Constants.KEY_OC_VERSION,
            AccountUtils.Constants.KEY_USER_ID
        )

        const val PAGE_SETTINGS = 0
        const val PAGE_ACCOUNTS = 1
        const val PAGE_JOBS = 2
        const val PAGE_MIGRATIONS = 3
    }

    /**
     * This data class holds all relevant account information that is
     * otherwise kept in separate collections.
     */
    data class AccountData(val account: Account, val userData: Map<String, String?>)

    val currentUser: User get() = accountManager.user
    val currentPage: LiveData<EtmMenuEntry?> = MutableLiveData()
    val pages: List<EtmMenuEntry> = listOf(
        EtmMenuEntry(
            iconRes = R.drawable.ic_settings,
            titleRes = R.string.etm_preferences,
            pageClass = EtmPreferencesFragment::class
        ),
        EtmMenuEntry(
            iconRes = R.drawable.ic_user,
            titleRes = R.string.etm_accounts,
            pageClass = EtmAccountsFragment::class
        ),
        EtmMenuEntry(
            iconRes = R.drawable.ic_clock,
            titleRes = R.string.etm_background_jobs,
            pageClass = EtmBackgroundJobsFragment::class
        ),
        EtmMenuEntry(
            iconRes = R.drawable.ic_arrow_up,
            titleRes = R.string.etm_migrations,
            pageClass = EtmMigrations::class
        ),
        EtmMenuEntry(
            iconRes = R.drawable.ic_cloud_download,
            titleRes = R.string.etm_transfer,
            pageClass = EtmFileTransferFragment::class
        )
    )
    val transferManagerConnection = TransferManagerConnection(context, accountManager.user)

    val preferences: Map<String, String> get() {
        return defaultPreferences.all
            .map { it.key to "${it.value}" }
            .sortedBy { it.first }
            .toMap()
    }

    val accounts: List<AccountData> get() {
        val accountType = resources.getString(R.string.account_type)
        return platformAccountManager.getAccountsByType(accountType).map { account ->
            val userData: Map<String, String?> = ACCOUNT_USER_DATA_KEYS.map { key ->
                key to platformAccountManager.getUserData(account, key)
            }.toMap()
            AccountData(account, userData)
        }
    }

    val backgroundJobs: LiveData<List<JobInfo>> get() {
        return backgroundJobManager.jobs
    }

    val migrationsInfo: List<MigrationInfo> get() {
        return migrationsManager.info
    }

    val migrationsStatus: MigrationsManager.Status get() {
        return migrationsManager.status.value ?: MigrationsManager.Status.UNKNOWN
    }

    val lastMigratedVersion: Int get() {
        return migrationsDb.lastMigratedVersion
    }

    init {
        (currentPage as MutableLiveData).apply {
            value = null
        }
    }

    fun onPageSelected(index: Int) {
        if (index < pages.size) {
            currentPage as MutableLiveData
            currentPage.value = pages[index]
        }
    }

    fun onBackPressed(): Boolean {
        (currentPage as MutableLiveData)
        return if (currentPage.value != null) {
            currentPage.value = null
            true
        } else {
            false
        }
    }

    fun pruneJobs() {
        backgroundJobManager.pruneJobs()
    }

    fun cancelAllJobs() {
        backgroundJobManager.cancelAllJobs()
    }

    fun startTestJob(periodic: Boolean) {
        if (periodic) {
            backgroundJobManager.scheduleTestJob()
        } else {
            backgroundJobManager.startImmediateTestJob()
        }
    }

    fun cancelTestJob() {
        backgroundJobManager.cancelTestJob()
    }

    fun clearMigrations() {
        migrationsDb.clearMigrations()
    }
}
