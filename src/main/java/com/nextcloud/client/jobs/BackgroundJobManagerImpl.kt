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
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
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
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
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
    private val clock: Clock
) : BackgroundJobManager {

    companion object {
        const val TAG_ALL = "*" // This tag allows us to retrieve list of all jobs run by Nextcloud client
        const val JOB_CONTENT_OBSERVER = "content_observer"
        const val JOB_PERIODIC_CONTACTS_BACKUP = "periodic_contacts_backup"
        const val JOB_IMMEDIATE_CONTACTS_BACKUP = "immediate_contacts_backup"
        const val JOB_IMMEDIATE_CONTACTS_IMPORT = "immediate_contacts_import"
        const val JOB_PERIODIC_FILES_SYNC = "periodic_files_sync"
        const val JOB_IMMEDIATE_FILES_SYNC = "immediate_files_sync"
        const val JOB_PERIODIC_OFFLINE_SYNC = "periodic_offline_sync"
        const val JOB_PERIODIC_MEDIA_FOLDER_DETECTION = "periodic_media_folder_detection"
        const val JOB_IMMEDIATE_MEDIA_FOLDER_DETECTION = "immediate_media_folder_detection"
        const val JOB_NOTIFICATION = "notification"
        const val JOB_ACCOUNT_REMOVAL = "account_removal"

        const val JOB_TEST = "test_job"

        const val MAX_CONTENT_TRIGGER_DELAY_MS = 1500L

        const val TAG_PREFIX_NAME = "name"
        const val TAG_PREFIX_USER = "user"
        const val TAG_PREFIX_START_TIMESTAMP = "timestamp"
        val PREFIXES = setOf(TAG_PREFIX_NAME, TAG_PREFIX_USER, TAG_PREFIX_START_TIMESTAMP)
        const val NOT_SET_VALUE = "not set"
        const val PERIODIC_CONTACTS_BACKUP_INTERVAL_MINUTES = 24 * 60L
        const val DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES = 15L
        const val DEFAULT_IMMEDIATE_JOB_DELAY_SEC = 3L

        fun formatNameTag(name: String, user: User? = null): String {
            return if (user == null) {
                "$TAG_PREFIX_NAME:$name"
            } else {
                "$TAG_PREFIX_NAME:$name ${user.accountName}"
            }
        }
        fun formatUserTag(user: User): String = "$TAG_PREFIX_USER:${user.accountName}"
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
            try {
                val ms = timestamp.toLong()
                return Date(ms)
            } catch (ex: NumberFormatException) {
                return Date(0)
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
                    progress = info.progress.getInt("progress", -1)
                )
            } else {
                null
            }
        }
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
        user?.let { builder.addTag(formatUserTag(it)) }
        return builder
    }

    private fun WorkManager.getJobInfo(id: UUID): LiveData<JobInfo?> {
        val workInfo = getWorkInfoByIdLiveData(id)
        return Transformations.map(workInfo) { fromWorkInfo(it) }
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
            return Transformations.map(workInfo) {
                it.map { fromWorkInfo(it) ?: JobInfo() }.sortedBy { it.started }.reversed()
            }
        }

    @RequiresApi(Build.VERSION_CODES.N)
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
            .putString(ContactsBackupWork.ACCOUNT, user.accountName)
            .putBoolean(ContactsBackupWork.FORCE, true)
            .build()
        val request = periodicRequestBuilder(
            jobClass = ContactsBackupWork::class,
            jobName = JOB_PERIODIC_CONTACTS_BACKUP,
            intervalMins = PERIODIC_CONTACTS_BACKUP_INTERVAL_MINUTES,
            user = user
        ).setInputData(data).build()

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

    override fun startImmediateContactsBackup(user: User): LiveData<JobInfo?> {
        val data = Data.Builder()
            .putString(ContactsBackupWork.ACCOUNT, user.accountName)
            .putBoolean(ContactsBackupWork.FORCE, true)
            .build()

        val request = oneTimeRequestBuilder(ContactsBackupWork::class, JOB_IMMEDIATE_CONTACTS_BACKUP, user)
            .setInputData(data)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_CONTACTS_BACKUP, ExistingWorkPolicy.KEEP, request)
        return workManager.getJobInfo(request.id)
    }

    override fun schedulePeriodicFilesSyncJob() {
        val request = periodicRequestBuilder(
            jobClass = FilesSyncWork::class,
            jobName = JOB_PERIODIC_FILES_SYNC,
            intervalMins = DEFAULT_PERIODIC_JOB_INTERVAL_MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(JOB_PERIODIC_FILES_SYNC, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    override fun startImmediateFilesSyncJob(skipCustomFolders: Boolean, overridePowerSaving: Boolean) {
        val arguments = Data.Builder()
            .putBoolean(FilesSyncWork.SKIP_CUSTOM, skipCustomFolders)
            .putBoolean(FilesSyncWork.OVERRIDE_POWER_SAVING, overridePowerSaving)
            .build()

        val request = oneTimeRequestBuilder(
            jobClass = FilesSyncWork::class,
            jobName = JOB_IMMEDIATE_FILES_SYNC
        )
            .setInputData(arguments)
            .build()

        workManager.enqueueUniqueWork(JOB_IMMEDIATE_FILES_SYNC, ExistingWorkPolicy.KEEP, request)
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
}
