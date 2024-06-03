/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import androidx.lifecycle.LiveData
import androidx.work.ListenableWorker
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.DownloadType

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

    fun logStartOfWorker(workerName: String?)

    fun logEndOfWorker(workerName: String?, result: ListenableWorker.Result)

    /**
     * Start content observer job that monitors changes in media folders
     * and launches synchronization when needed.
     *
     * This call is idempotent - there will be only one scheduled job
     * regardless of number of calls.
     */
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
     * Schedule periodic calendar backups job. Operating system will
     * decide when to start the job.
     *
     * This call is idempotent - there can be only one scheduled job
     * at any given time.
     *
     * @param user User for which job will be scheduled.
     */
    fun schedulePeriodicCalendarBackup(user: User)

    /**
     * Cancel periodic calendar backup. Existing tasks might finish, but no new
     * invocations will occur.
     */
    fun cancelPeriodicCalendarBackup(user: User)

    /**
     * Immediately start single calendar backup job.
     * This job will launch independently from periodic calendar backup.
     *
     * @return Job info with current status; status is null if job does not exist
     */
    fun startImmediateCalendarBackup(user: User): LiveData<JobInfo?>

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

    /**
     * Immediately start calendar import job. Import job will be started only once.
     * If new job is started while existing job is running - request will be ignored
     * and currently running job will continue running.
     *
     * @param calendarPaths Array of paths of calendar files to import from
     *
     * @return Job info with current status; status is null if job does not exist
     */
    fun startImmediateCalendarImport(calendarPaths: Map<String, Int>): LiveData<JobInfo?>

    fun startImmediateFilesExportJob(files: Collection<OCFile>): LiveData<JobInfo?>

    fun schedulePeriodicFilesSyncJob(syncedFolderID: Long)

    /**
     * Immediately start File Sync job for given syncFolderID.
     */
    fun startImmediateFilesSyncJob(
        syncedFolderID: Long,
        overridePowerSaving: Boolean = false,
        changedFiles: Array<String> = arrayOf<String>()
    )

    fun scheduleOfflineSync()

    fun scheduleMediaFoldersDetectionJob()
    fun startMediaFoldersDetectionJob()

    fun startNotificationJob(subject: String, signature: String)
    fun startAccountRemovalJob(accountName: String, remoteWipe: Boolean)
    fun startFilesUploadJob(user: User)
    fun getFileUploads(user: User): LiveData<List<JobInfo>>
    fun cancelFilesUploadJob(user: User)
    fun isStartFileUploadJobScheduled(user: User): Boolean

    fun cancelFilesDownloadJob(user: User, fileId: Long)

    fun isStartFileDownloadJobScheduled(user: User, fileId: Long): Boolean

    @Suppress("LongParameterList")
    fun startFileDownloadJob(
        user: User,
        file: OCFile,
        behaviour: String,
        downloadType: DownloadType?,
        activityName: String,
        packageName: String,
        conflictUploadId: Long?
    )

    fun startPdfGenerateAndUploadWork(user: User, uploadFolder: String, imagePaths: List<String>, pdfPath: String)

    fun scheduleTestJob()
    fun startImmediateTestJob()
    fun cancelTestJob()

    fun pruneJobs()
    fun cancelAllJobs()
    fun schedulePeriodicHealthStatus()
    fun startHealthStatus()
    fun bothFilesSyncJobsRunning(syncedFolderID: Long): Boolean
}
