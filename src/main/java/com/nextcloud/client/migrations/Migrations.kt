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
package com.nextcloud.client.migrations

import android.os.Build
import androidx.work.WorkManager
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.logger.Logger
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import javax.inject.Inject

/**
 * This class collects all migration steps and provides API to supply those
 * steps to [MigrationsManager] for execution.
 */
class Migrations @Inject constructor(
    private val logger: Logger,
    private val userAccountManager: UserAccountManager,
    private val workManager: WorkManager,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val jobManager: BackgroundJobManager
) {

    companion object {
        val TAG = Migrations::class.java.simpleName
    }

    /**
     * This class wraps migration logic with some metadata with some
     * metadata required to register and log overall migration progress.
     *
     * @param id Step id; id must be unique; this is verified upon registration
     * @param description Human readable migration step descriptions
     * @param mandatory If true, failing migration will cause an exception; if false, it will be skipped and repeated
     *                  again on next startup
     * @throws Exception migration logic is permitted to throw any kind of exceptions; all exceptions will be wrapped
     * into [MigrationException]
     */
    abstract class Step(val id: Int, val description: String, val mandatory: Boolean = true) : Runnable

    /**
     * Migrate legacy accounts by adding user IDs. This migration can be re-tried until all accounts are
     * successfully migrated.
     */
    private val migrateUserId = object : Step(0, "Migrate user id", false) {
        override fun run() {
            val allAccountsHaveUserId = userAccountManager.migrateUserId()
            logger.i(TAG, "$description: success = $allAccountsHaveUserId")
            if (!allAccountsHaveUserId) {
                throw IllegalStateException("Failed to set user id for all accounts")
            }
        }
    }

    /**
     * Content observer job must be restarted to use new scheduler abstraction.
     */
    private val migrateContentObserverJob = object : Step(1, "Migrate content observer job", false) {
        override fun run() {
            val legacyWork = workManager.getWorkInfosByTag("content_sync").get()
            legacyWork.forEach {
                logger.i(TAG, "$description: cancelling legacy work ${it.id}")
                workManager.cancelWorkById(it.id)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                jobManager.scheduleContentObserverJob()
                logger.i(TAG, "$description: enabled")
            } else {
                logger.i(TAG, "$description: disabled")
            }
        }
    }

    /**
     * Contacts backup job has been migrated to new job runner framework. Re-start contacts upload
     * for all users that have it enabled.
     *
     * Old job is removed from source code, so we need to restart it for each user using
     * new jobs API.
     */
    private val migrateContactsBackupJob = object : Step(2, "Restart contacts backup job") {
        override fun run() {
            val users = userAccountManager.allUsers
            if (users.isEmpty()) {
                logger.i(TAG, "$description: no users to migrate")
            } else {
                users.forEach {
                    val backupEnabled = arbitraryDataProvider.getBooleanValue(
                        it.accountName,
                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP
                    )
                    if (backupEnabled) {
                        jobManager.schedulePeriodicContactsBackup(it)
                    }
                    logger.i(TAG, "$description: user = ${it.accountName}, backup enabled = $backupEnabled")
                }
            }
        }
    }

    /**
     * List of migration steps. Those steps will be loaded and run by [MigrationsManager]
     */
    val steps: List<Step> = listOf(
        migrateUserId,
        migrateContentObserverJob,
        migrateContactsBackupJob
    ).sortedBy { it.id }.apply {
        val uniqueIds = associateBy { it.id }.size
        if (uniqueIds != size) {
            throw IllegalStateException("All migrations must have unique id")
        }
    }
}
