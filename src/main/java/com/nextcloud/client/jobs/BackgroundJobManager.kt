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
package com.nextcloud.client.jobs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.nextcloud.client.account.User

/**
 * This interface allows to control, schedule and monitor all application
 * long-running background tasks, such as periodic checks or synchronization.
 */
@Suppress("TooManyFunctions") // we expect this implementation to have rich API
interface BackgroundJobManager {

    /**
     * Information about all application background jobs.
     */
    val jobs: LiveData<List<JobInfo>>

    /**
     * Start content observer job that monitors changes in media folders
     * and launches synchronization when needed.
     *
     * This call is idempotent - there will be only one scheduled job
     * regardless of number of calls.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun scheduleContentObserverJob()

    /**
     * Schedule periodic contacts backups job. Operating system will
     * decide when to start the job.
     *
     * This call is idempotent - there can be only one scheduled job
     * at any given time.
     *
     * @param user User for which job will be scheduled.
     */
    fun schedulePeriodicContactsBackup(user: User)

    /**
     * Cancel periodic contacts backup. Existing tasks might finish, but no new
     * invocations will occur.
     */
    fun cancelPeriodicContactsBackup(user: User)

    /**
     * Immediately start single contacts backup job.
     * This job will launch independently from periodic contacts backup.
     *
     * @return Job info with current status; status is null if job does not exist
     */
    fun startImmediateContactsBackup(user: User): LiveData<JobInfo?>

    /**
     * Immediately start contacts import job. Import job will be started only once.
     * If new job is started while existing job is running - request will be ignored
     * and currently running job will continue running.
     *
     * @param contactsAccountName Target contacts account name; null for local contacts
     * @param contactsAccountType Target contacts account type; null for local contacts
     * @param vCardFilePath Path to file containing all contact entries
     * @param selectedContacts List of contact indices to import from [vCardFilePath] file
     *
     * @return Job info with current status; status is null if job does not exist
     */
    fun startImmediateContactsImport(
        contactsAccountName: String?,
        contactsAccountType: String?,
        vCardFilePath: String,
        selectedContacts: IntArray
    ): LiveData<JobInfo?>

    fun schedulePeriodicFilesSyncJob()
    fun startImmediateFilesSyncJob(skipCustomFolders: Boolean = false, overridePowerSaving: Boolean = false)
    fun scheduleOfflineSync()

    fun scheduleMediaFoldersDetectionJob()
    fun startMediaFoldersDetectionJob()

    fun startNotificationJob(subject: String, signature: String)
    fun startAccountRemovalJob(accountName: String, remoteWipe: Boolean)

    fun scheduleTestJob()
    fun startImmediateTestJob()
    fun cancelTestJob()

    fun pruneJobs()
    fun cancelAllJobs()
}
