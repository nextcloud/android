/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

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
    class Step(val id: Int, val description: String, val mandatory: Boolean = true, val run: (s: Step) -> Unit) {
        override fun toString(): String = "Migration $id: $description"
    }

    /**
     * NOP migration used to replace applied migrations that should be applied again.
     */
    private fun nop(s: Step) {
        logger.i(TAG, "$s: skipped deprecated migration")
    }

    /**
     * Migrate legacy accounts by adding user IDs. This migration can be re-tried until all accounts are
     * successfully migrated.
     */
    private fun migrateUserId(s: Step) {
        val allAccountsHaveUserId = userAccountManager.migrateUserId()
        logger.i(TAG, "${s.description}: success = $allAccountsHaveUserId")
        if (!allAccountsHaveUserId) {
            throw IllegalStateException("Failed to set user id for all accounts")
        }
    }

    /**
     * Content observer job must be restarted to use new scheduler abstraction.
     */
    private fun migrateContentObserverJob(s: Step) {
        val legacyWork = workManager.getWorkInfosByTag("content_sync").get()
        legacyWork.forEach {
            logger.i(TAG, "${s.description}: cancelling legacy work ${it.id}")
            workManager.cancelWorkById(it.id)
        }
        jobManager.scheduleContentObserverJob()
        logger.i(TAG, "$s: enabled")
    }

    /**
     * Periodic contacts backup job has been changed and should be restarted.
     */
    private fun restartContactsBackupJobs(s: Step) {
        val users = userAccountManager.allUsers
        if (users.isEmpty()) {
            logger.i(TAG, "$s: no users to migrate")
        } else {
            users.forEach {
                val backupEnabled = arbitraryDataProvider.getBooleanValue(
                    it.accountName,
                    ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP
                )
                if (backupEnabled) {
                    jobManager.schedulePeriodicContactsBackup(it)
                }
                logger.i(TAG, "$s: user = ${it.accountName}, backup enabled = $backupEnabled")
            }
        }
    }

    /**
     * List of migration steps. Those steps will be loaded and run by [MigrationsManager].
     *
     * If a migration should be run again (applicable to periodic job restarts), insert
     * the migration with new ID. To prevent accidental re-use of older IDs, replace old
     * migration with this::nop.
     */
    @Suppress("MagicNumber")
    val steps: List<Step> = listOf(
        Step(0, "Migrate user id", false, this::migrateUserId),
        Step(1, "Migrate content observer job", false, this::migrateContentObserverJob),
        Step(2, "Restart contacts backup job", true, this::nop),
        Step(3, "Restart contacts backup job", true, this::restartContactsBackupJobs)
    ).sortedBy { it.id }.apply {
        val uniqueIds = associateBy { it.id }.size
        if (uniqueIds != size) {
            throw IllegalStateException("All migrations must have unique id")
        }
    }
}
