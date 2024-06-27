/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.documentscan.GeneratePdfFromImagesWork
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.isWorkRunning
import com.nextcloud.utils.extensions.isWorkScheduled
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.DownloadType
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Note to maintainers
 *
 * Since [androidx.work.WorkManager] is missing API to easily attach worker metadata,
 * we use tags API to attach our custom metadata.
 *
 * To create new job request, use [BackgroundJobManagerImpl.oneTimeRequestBuilder] and
 * [BackgroundJobManagerImpl.periodicRequestBuilder] calls, instead of calling
 * platform builders. Those methods will create builders pre-set with mandatory tags.
 *
 * Since Google is notoriously releasing new background job services, [androidx.work.WorkManager] API is
 * considered private implementation detail and should not be leaked through the interface, to minimize
 * potential migration cost in the future.
 */
@Suppress("TooManyFunctions") // we expect this implementation to have rich API
internal class BackgroundJobManagerImpl(
    private val workManager: WorkManager,
    private val clock: Clock,
    private val preferences: AppPreferences
) : BackgroundJobManager, Injectable {

    companion object {

        const val TAG_ALL = "*" // This tag allows us to retrieve list of all jobs run by Nextcloud client
        const val JOB_CONTENT_OBSERVER = "content_observer"
        const val JOB_PERIODIC_CONTACTS_BACKUP = "periodic_contacts_backup"
        const val JOB_IMMEDIATE_CONTACTS_BACKUP = "immediate_contacts_backup"
        const val JOB_IMMEDIATE_CONTACTS_IMPORT = "immediate_contacts_import"
        const val JOB_PERIODIC_CALENDAR_BACKUP = "periodic_calendar_backup"
        const val JOB_IMMEDIATE_CALENDAR_IMPORT = "immediate_calendar_import"
        const val JOB_PERIODIC_FILES_SYNC = "periodic_files_sync"
        const val JOB_IMMEDIATE_FILES_SYNC = "immediate_files_sync"
        const val JOB_PERIODIC_OFFLINE_SYNC = "periodic_offline_sync"
        const val JOB_PERIODIC_MEDIA_FOLDER_DETECTION = "periodic_media_folder_detection"
        const val JOB_IMMEDIATE_MEDIA_FOLDER_DETECTION = "immediate_media_folder_detection"
        const val JOB_NOTIFICATION = "notification"
        const val JOB_ACCOUNT_REMOVAL = "account_removal"
        const val JOB_FILES_UPLOAD = "files_upload"
        const val JOB_FOLDER_DOWNLOAD = "folder_download"
        const val JOB_FILES_DOWNLOAD = "files_download"
        const val JOB_PDF_GENERATION = "pdf_generation"
        const val JOB_IMMEDIATE_CALENDAR_BACKUP = "immediate_calendar_backup"
        const val JOB_IMMEDIATE_FILES_EXPORT = "immediate_files_export"

        const val JOB_PERIODIC_HEALTH_STATUS = "periodic_health_status"
        const val JOB_IMMEDIATE_HEALTH_STATUS = "immediate_health_status"

        const val JOB_TEST = "test_job"

        const val MAX_CONTENT_TRIGGER_DELAY_MS = 10000L

        const val TAG_PREFIX_NAME = "name"
        const val TAG_PREFIX_USER = "user"
        const val TAG_PREFIX_CLASS = "class"
        const val TAG_PREFIX_START_TIMESTAMP = "timestamp"
        val PREFIXES = setOf(TAG_PREFIX_NAME, TAG_PREFIX_USER, TAG_PREFIX_START_TIMESTAMP, TAG_PREFIX_CLASS)
        const val NOT_SET_VALUE = "not set"
        const val PERIODIC_BACKUP_INTERVAL_MINUTES = 24 * 60L
        const val DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES = 15L
        const val DEFAULT_IMMEDIATE_JOB_DELAY_SEC = 3L

        private const val KEEP_LOG_MILLIS = 1000 * 60 * 60 * 24 * 3L

        fun formatNameTag(name: String, user: User? = null): String {
            return if (user == null) {
                "$TAG_PREFIX_NAME:$name"
            } else {
                "$TAG_PREFIX_NAME:$name ${user.accountName}"
            }
        }

        fun formatUserTag(user: User): String = "$TAG_PREFIX_USER:${user.accountName}"
        fun formatClassTag(jobClass: KClass<out ListenableWorker>): String = "$TAG_PREFIX_CLASS:${jobClass.simpleName}"
        fun formatTimeTag(startTimestamp: Long): String = "$TAG_PREFIX_START_TIMESTAMP:$startTimestamp"

        fun parseTag(tag: String): Pair<String, String>? {
            val key = tag.substringBefore(":", "")
            val value = tag.substringAfter(":", "")
            return if (key in PREFIXES) {
                key to value
            } else {
                null
            }
        }

        fun parseTimestamp(timestamp: String): Date {
            return try {
                val ms = timestamp.toLong()
                Date(ms)
            } catch (ex: NumberFormatException) {
                Date(0)
            }
        }

        /**
         * Convert platform [androidx.work.WorkInfo] object into application-specific [JobInfo] model.
         * Conversion extracts work metadata from tags.
         */
        fun fromWorkInfo(info: WorkInfo?): JobInfo? {
            return if (info != null) {
                val metadata = mutableMapOf<String, String>()
                info.tags.forEach { parseTag(it)?.let { metadata[it.first] = it.second } }
                val timestamp = parseTimestamp(metadata.get(TAG_PREFIX_START_TIMESTAMP) ?: "0")
                JobInfo(
                    id = info.id,
                    state = info.state.toString(),
                    name = metadata.get(TAG_PREFIX_NAME) ?: NOT_SET_VALUE,
                    user = metadata.get(TAG_PREFIX_USER) ?: NOT_SET_VALUE,
                    started = timestamp,
                    progress = info.progress.getInt("progress", -1),
                    workerClass = metadata.get(TAG_PREFIX_CLASS) ?: NOT_SET_VALUE
                )
            } else {
                null
            }
        }

        fun deleteOldLogs(logEntries: MutableList<LogEntry>): MutableList<LogEntry> {
            logEntries.removeIf {
                return@removeIf (
                    it.started != null &&
                        Date(Date().time - KEEP_LOG_MILLIS).after(it.started)
                    ) ||
                    (
                        it.finished != null &&
                            Date(Date().time - KEEP_LOG_MILLIS).after(it.finished)
                        )
            }
            return logEntries
        }
    }

    override fun logStartOfWorker(workerName: String?) {
        val logs = deleteOldLogs(preferences.readLogEntry().toMutableList())

        if (workerName == null) {
            logs.add(LogEntry(Date(), null, null, NOT_SET_VALUE))
        } else {
            logs.add(LogEntry(Date(), null, null, workerName))
        }
        preferences.saveLogEntry(logs)
    }

    override fun logEndOfWorker(workerName: String?, result: ListenableWorker.Result) {
        val logs = deleteOldLogs(preferences.readLogEntry().toMutableList())
        if (workerName == null) {
            logs.add(LogEntry(null, Date(), result.toString(), NOT_SET_VALUE))
        } else {
            logs.add(LogEntry(null, Date(), result.toString(), workerName))
        }
        preferences.saveLogEntry(logs)
    }

    /**
     * Create [OneTimeWorkRequest.Builder] pre-set with common attributes
     */
    private fun oneTimeRequestBuilder(
        jobClass: KClass<out ListenableWorker>,
        jobName: String,
        user: User? = null
    ): OneTimeWorkRequest.Builder {
        val builder = OneTimeWorkRequest.Builder(jobClass.java)
            .addTag(TAG_ALL)
            .addTag(formatNameTag(jobName, user))
            .addTag(formatTimeTag(clock.currentTime))
            .addTag(formatClassTag(jobClass))
        user?.let { builder.addTag(formatUserTag(it)) }
        return builder
    }

    /**
     * Create [PeriodicWorkRequest] pre-set with common attributes
     */
    private fun periodicRequestBuilder(
        jobClass: KClass<out ListenableWorker>,
        jobName: String,
        intervalMins: Long = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES,
        flexIntervalMins: Long = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES,
        user: User? = null
    ): PeriodicWorkRequest.Builder {
        val builder = PeriodicWorkRequest.Builder(
            jobClass.java,
            intervalMins,
            TimeUnit.MINUTES,
            flexIntervalMins,
            TimeUnit.MINUTES
        )
            .addTag(TAG_ALL)
            .addTag(formatNameTag(jobName, user))
            .addTag(formatTimeTag(clock.currentTime))
            .addTag(formatClassTag(jobClass))
        user?.let { builder.addTag(formatUserTag(it)) }
        return builder
    }

    private fun WorkManager.getJobInfo(id: UUID): LiveData<JobInfo?> {
        val workInfo = getWorkInfoByIdLiveData(id)
        return workInfo.map { fromWorkInfo(it) }
    }

    /**
     * Cancel work using name tag with optional user scope.
     * All work instances will be cancelled.
     */
    private fun WorkManager.cancelJob(name: String, user: User? = null): Operation {
        val tag = formatNameTag(name, user)
        return cancelAllWorkByTag(tag)
    }

    override val jobs: LiveData<List<JobInfo>>
        get() {
            val workInfo = workManager.getWorkInfosByTagLiveData("*")
            return workInfo.map { it -> it.map { fromWorkInfo(it) ?: JobInfo() }.sortedBy { it.started }.reversed() }
        }

    override fun scheduleContentObserverJob() {
        val constrains = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentMaxDelay(MAX_CONTENT_TRIGGER_DELAY_MS, TimeUnit.MILLISECONDS)
            .build()

        val request = oneTimeRequestBuilder(ContentObserverWork::class, JOB_CONTENT_OBSERVER)
            .setConstraints(constrains)
            .build()

        workManager.enqueueUniqueWork(JOB_CONTENT_OBSERVER, ExistingWorkPolicy.REPLACE, request)
    }

    override fun schedulePeriodicContactsBackup(user: User) {
        val data = Data.Builder()
            .putString(ContactsBackupWork.KEY_ACCOUNT, user.accountName)
            .putBoolean(ContactsBackupWork.KEY_FORCE, true)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = periodicRequestBuilder(
            jobClass = ContactsBackupWork::class,
            jobName = JOB_PERIODIC_CONTACTS_BACKUP,
            intervalMins = PERIODIC_BACKUP_INTERVAL_MINUTES,
            user = user
        )
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(JOB_PERIODIC_CONTACTS_BACKUP, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelPeriodicContactsBackup(user: User) {
        workManager.cancelJob(JOB_PERIODIC_CONTACTS_BACKUP, user)
    }

    override fun startImmediateContactsImport(
        contactsAccountName: String?,
        contactsAccountType: String?,
        vCardFilePath: String,
        selectedContacts: IntArray
    ): LiveData<JobInfo?> {
        val data = Data.Builder()
            .putString(ContactsImportWork.ACCOUNT_NAME, contactsAccountName)
            .putString(ContactsImportWork.ACCOUNT_TYPE, contactsAccountType)
            .putString(ContactsImportWork.VCARD_FILE_PATH, vCardFilePath)
            .putIntArray(ContactsImportWork.SELECTED_CONTACTS_INDICES, selectedContacts)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .build()

        val request = oneTimeRequestBuilder(ContactsImportWork::class, JOB_IMMEDIATE_CONTACTS_IMPORT)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_CONTACTS_IMPORT, ExistingWorkPolicy.KEEP, request)

        return workManager.getJobInfo(request.id)
    }

    override fun startImmediateCalendarImport(calendarPaths: Map<String, Int>): LiveData<JobInfo?> {
        val data = Data.Builder()
            .putAll(calendarPaths)
            .build()

        val constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .build()

        val request = oneTimeRequestBuilder(CalendarImportWork::class, JOB_IMMEDIATE_CALENDAR_IMPORT)
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_CALENDAR_IMPORT, ExistingWorkPolicy.KEEP, request)

        return workManager.getJobInfo(request.id)
    }

    override fun startImmediateFilesExportJob(files: Collection<OCFile>): LiveData<JobInfo?> {
        val ids = files.map { it.fileId }.toLongArray()

        val data = Data.Builder()
            .putLongArray(FilesExportWork.FILES_TO_DOWNLOAD, ids)
            .build()

        val request = oneTimeRequestBuilder(FilesExportWork::class, JOB_IMMEDIATE_FILES_EXPORT)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_FILES_EXPORT, ExistingWorkPolicy.APPEND_OR_REPLACE, request)

        return workManager.getJobInfo(request.id)
    }

    override fun startImmediateContactsBackup(user: User): LiveData<JobInfo?> {
        val data = Data.Builder()
            .putString(ContactsBackupWork.KEY_ACCOUNT, user.accountName)
            .putBoolean(ContactsBackupWork.KEY_FORCE, true)
            .build()

        val request = oneTimeRequestBuilder(ContactsBackupWork::class, JOB_IMMEDIATE_CONTACTS_BACKUP, user)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_CONTACTS_BACKUP, ExistingWorkPolicy.KEEP, request)
        return workManager.getJobInfo(request.id)
    }

    override fun startImmediateCalendarBackup(user: User): LiveData<JobInfo?> {
        val data = Data.Builder()
            .putString(CalendarBackupWork.ACCOUNT, user.accountName)
            .putBoolean(CalendarBackupWork.FORCE, true)
            .build()

        val request = oneTimeRequestBuilder(CalendarBackupWork::class, JOB_IMMEDIATE_CALENDAR_BACKUP, user)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_CALENDAR_BACKUP, ExistingWorkPolicy.KEEP, request)
        return workManager.getJobInfo(request.id)
    }

    override fun schedulePeriodicCalendarBackup(user: User) {
        val data = Data.Builder()
            .putString(CalendarBackupWork.ACCOUNT, user.accountName)
            .putBoolean(CalendarBackupWork.FORCE, true)
            .build()
        val request = periodicRequestBuilder(
            jobClass = CalendarBackupWork::class,
            jobName = JOB_PERIODIC_CALENDAR_BACKUP,
            intervalMins = PERIODIC_BACKUP_INTERVAL_MINUTES,
            user = user
        ).setInputData(data).build()

        workManager.enqueueUniquePeriodicWork(JOB_PERIODIC_CALENDAR_BACKUP, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancelPeriodicCalendarBackup(user: User) {
        workManager.cancelJob(JOB_PERIODIC_CALENDAR_BACKUP, user)
    }

    override fun bothFilesSyncJobsRunning(syncedFolderID: Long): Boolean {
        return workManager.isWorkRunning(JOB_PERIODIC_FILES_SYNC + "_" + syncedFolderID) &&
            workManager.isWorkRunning(JOB_IMMEDIATE_FILES_SYNC + "_" + syncedFolderID)
    }

    override fun schedulePeriodicFilesSyncJob(syncedFolderID: Long) {
        val arguments = Data.Builder()
            .putLong(FilesSyncWork.SYNCED_FOLDER_ID, syncedFolderID)
            .build()

        val request = periodicRequestBuilder(
            jobClass = FilesSyncWork::class,
            jobName = JOB_PERIODIC_FILES_SYNC + "_" + syncedFolderID,
            intervalMins = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES
        )
            .setInputData(arguments)
            .build()
        workManager.enqueueUniquePeriodicWork(
            JOB_PERIODIC_FILES_SYNC + "_" + syncedFolderID,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    override fun startImmediateFilesSyncJob(
        syncedFolderID: Long,
        overridePowerSaving: Boolean,
        changedFiles: Array<String>
    ) {
        val arguments = Data.Builder()
            .putBoolean(FilesSyncWork.OVERRIDE_POWER_SAVING, overridePowerSaving)
            .putStringArray(FilesSyncWork.CHANGED_FILES, changedFiles)
            .putLong(FilesSyncWork.SYNCED_FOLDER_ID, syncedFolderID)
            .build()

        val request = oneTimeRequestBuilder(
            jobClass = FilesSyncWork::class,
            jobName = JOB_IMMEDIATE_FILES_SYNC + "_" + syncedFolderID
        )
            .setInputData(arguments)
            .build()

        workManager.enqueueUniqueWork(
            JOB_IMMEDIATE_FILES_SYNC + "_" + syncedFolderID,
            ExistingWorkPolicy.APPEND,
            request
        )
    }

    override fun scheduleOfflineSync() {
        val constrains = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val request = periodicRequestBuilder(OfflineSyncWork::class, JOB_PERIODIC_OFFLINE_SYNC)
            .setConstraints(constrains)
            .build()

        workManager.enqueueUniquePeriodicWork(JOB_PERIODIC_OFFLINE_SYNC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun scheduleMediaFoldersDetectionJob() {
        val request = periodicRequestBuilder(MediaFoldersDetectionWork::class, JOB_PERIODIC_MEDIA_FOLDER_DETECTION)
            .build()

        workManager.enqueueUniquePeriodicWork(
            JOB_PERIODIC_MEDIA_FOLDER_DETECTION,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override fun startMediaFoldersDetectionJob() {
        val request = oneTimeRequestBuilder(MediaFoldersDetectionWork::class, JOB_IMMEDIATE_MEDIA_FOLDER_DETECTION)
            .build()

        workManager.enqueueUniqueWork(
            JOB_IMMEDIATE_MEDIA_FOLDER_DETECTION,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun startNotificationJob(subject: String, signature: String) {
        val data = Data.Builder()
            .putString(NotificationWork.KEY_NOTIFICATION_SUBJECT, subject)
            .putString(NotificationWork.KEY_NOTIFICATION_SIGNATURE, signature)
            .build()

        val request = oneTimeRequestBuilder(NotificationWork::class, JOB_NOTIFICATION)
            .setInputData(data)
            .build()

        workManager.enqueue(request)
    }

    override fun startAccountRemovalJob(accountName: String, remoteWipe: Boolean) {
        val data = Data.Builder()
            .putString(AccountRemovalWork.ACCOUNT, accountName)
            .putBoolean(AccountRemovalWork.REMOTE_WIPE, remoteWipe)
            .build()

        val request = oneTimeRequestBuilder(AccountRemovalWork::class, JOB_ACCOUNT_REMOVAL)
            .setInputData(data)
            .build()

        workManager.enqueue(request)
    }

    private fun startFileUploadJobTag(user: User): String {
        return JOB_FILES_UPLOAD + user.accountName
    }

    override fun isStartFileUploadJobScheduled(user: User): Boolean {
        return workManager.isWorkScheduled(startFileUploadJobTag(user))
    }

    override fun startFilesUploadJob(user: User) {
        val data = workDataOf(FileUploadWorker.ACCOUNT to user.accountName)

        val tag = startFileUploadJobTag(user)

        val request = oneTimeRequestBuilder(FileUploadWorker::class, JOB_FILES_UPLOAD, user)
            .addTag(tag)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, request)
    }

    private fun startFileDownloadJobTag(user: User, fileId: Long): String {
        return JOB_FOLDER_DOWNLOAD + user.accountName + fileId
    }

    override fun isStartFileDownloadJobScheduled(user: User, fileId: Long): Boolean {
        return workManager.isWorkScheduled(startFileDownloadJobTag(user, fileId))
    }

    override fun startFileDownloadJob(
        user: User,
        file: OCFile,
        behaviour: String,
        downloadType: DownloadType?,
        activityName: String,
        packageName: String,
        conflictUploadId: Long?
    ) {
        val tag = startFileDownloadJobTag(user, file.fileId)

        val data = workDataOf(
            FileDownloadWorker.ACCOUNT_NAME to user.accountName,
            FileDownloadWorker.FILE_REMOTE_PATH to file.remotePath,
            FileDownloadWorker.BEHAVIOUR to behaviour,
            FileDownloadWorker.DOWNLOAD_TYPE to downloadType.toString(),
            FileDownloadWorker.ACTIVITY_NAME to activityName,
            FileDownloadWorker.PACKAGE_NAME to packageName,
            FileDownloadWorker.CONFLICT_UPLOAD_ID to conflictUploadId
        )

        val request = oneTimeRequestBuilder(FileDownloadWorker::class, JOB_FILES_DOWNLOAD, user)
            .addTag(tag)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.REPLACE, request)
    }

    override fun getFileUploads(user: User): LiveData<List<JobInfo>> {
        val workInfo = workManager.getWorkInfosByTagLiveData(formatNameTag(JOB_FILES_UPLOAD, user))
        return workInfo.map { it -> it.map { fromWorkInfo(it) ?: JobInfo() } }
    }

    override fun cancelFilesUploadJob(user: User) {
        workManager.cancelJob(JOB_FILES_UPLOAD, user)
    }

    override fun cancelFilesDownloadJob(user: User, fileId: Long) {
        workManager.cancelAllWorkByTag(startFileDownloadJobTag(user, fileId))
    }

    override fun startPdfGenerateAndUploadWork(
        user: User,
        uploadFolder: String,
        imagePaths: List<String>,
        pdfPath: String
    ) {
        val data = workDataOf(
            GeneratePdfFromImagesWork.INPUT_IMAGE_FILE_PATHS to imagePaths.toTypedArray(),
            GeneratePdfFromImagesWork.INPUT_OUTPUT_FILE_PATH to pdfPath,
            GeneratePdfFromImagesWork.INPUT_UPLOAD_ACCOUNT to user.accountName,
            GeneratePdfFromImagesWork.INPUT_UPLOAD_FOLDER to uploadFolder
        )
        val request = oneTimeRequestBuilder(GeneratePdfFromImagesWork::class, JOB_PDF_GENERATION)
            .setInputData(data)
            .build()
        workManager.enqueue(request)
    }

    override fun scheduleTestJob() {
        val request = periodicRequestBuilder(TestJob::class, JOB_TEST)
            .setInitialDelay(DEFAULT_IMMEDIATE_JOB_DELAY_SEC, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(JOB_TEST, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    override fun startImmediateTestJob() {
        val request = oneTimeRequestBuilder(TestJob::class, JOB_TEST)
            .build()
        workManager.enqueueUniqueWork(JOB_TEST, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelTestJob() {
        workManager.cancelAllWorkByTag(formatNameTag(JOB_TEST))
    }

    override fun pruneJobs() {
        workManager.pruneWork()
    }

    override fun cancelAllJobs() {
        workManager.cancelAllWorkByTag(TAG_ALL)
    }

    override fun schedulePeriodicHealthStatus() {
        val request = periodicRequestBuilder(
            jobClass = HealthStatusWork::class,
            jobName = JOB_PERIODIC_HEALTH_STATUS,
            intervalMins = PERIODIC_BACKUP_INTERVAL_MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(JOB_PERIODIC_HEALTH_STATUS, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun startHealthStatus() {
        val request = oneTimeRequestBuilder(HealthStatusWork::class, JOB_IMMEDIATE_HEALTH_STATUS)
            .build()

        workManager.enqueueUniqueWork(
            JOB_IMMEDIATE_HEALTH_STATUS,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
