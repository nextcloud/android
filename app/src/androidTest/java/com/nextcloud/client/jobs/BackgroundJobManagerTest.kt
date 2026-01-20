/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.annotation.UiThreadTest
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.toByteArray
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.io.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.ArgumentMatcher
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * When using IDE to run enire Suite, make sure tests are run using Android Instrumentation Test
 * runner. By default IDE runs normal JUnit - this is AS problem. One must configure the
 * test run manually.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    BackgroundJobManagerTest.Manager::class,
    BackgroundJobManagerTest.ContentObserver::class,
    BackgroundJobManagerTest.PeriodicContactsBackup::class,
    BackgroundJobManagerTest.ImmediateContactsBackup::class,
    BackgroundJobManagerTest.ImmediateContactsImport::class,
    BackgroundJobManagerTest.FilesUpload::class,
    BackgroundJobManagerTest.Tags::class
)
class BackgroundJobManagerTest {

    /**
     * Used to help with ambiguous type inference
     */
    class IsOneTimeWorkRequest : ArgumentMatcher<OneTimeWorkRequest> {
        override fun matches(argument: OneTimeWorkRequest?): Boolean = true
    }

    /**
     * Used to help with ambiguous type inference
     */
    class IsPeriodicWorkRequest : ArgumentMatcher<PeriodicWorkRequest> {
        override fun matches(argument: PeriodicWorkRequest?): Boolean = true
    }

    abstract class Fixture {
        companion object {
            internal const val USER_ACCOUNT_NAME = "user@nextcloud"
            internal val TIMESTAMP = System.currentTimeMillis()
        }
        internal lateinit var user: User
        internal lateinit var workManager: WorkManager
        internal lateinit var clock: Clock
        internal lateinit var preferences: AppPreferences
        internal lateinit var backgroundJobManager: BackgroundJobManagerImpl
        internal lateinit var context: Context

        @Before
        fun setUpFixture() {
            context = mock()
            user = mock()
            whenever(user.accountName).thenReturn(USER_ACCOUNT_NAME)
            workManager = mock()
            clock = mock()
            preferences = mock()
            whenever(clock.currentTime).thenReturn(TIMESTAMP)
            whenever(clock.currentDate).thenReturn(Date(TIMESTAMP))
            backgroundJobManager = BackgroundJobManagerImpl(workManager, clock, preferences)
        }

        fun assertHasRequiredTags(tags: Set<String>, jobName: String, user: User? = null) {
            assertTrue("""'all' tag is mandatory""", tags.contains("*"))
            assertTrue("name tag is mandatory", tags.contains(BackgroundJobManagerImpl.formatNameTag(jobName, user)))
            assertTrue("timestamp tag is mandatory", tags.contains(BackgroundJobManagerImpl.formatTimeTag(TIMESTAMP)))
            if (user != null) {
                assertTrue("user tag is mandatory", tags.contains(BackgroundJobManagerImpl.formatUserTag(user)))
            }
        }

        fun buildWorkInfo(index: Long): WorkInfo = WorkInfo(
            id = UUID.randomUUID(),
            state = WorkInfo.State.RUNNING,
            outputData = Data.Builder().build(),
            tags = setOf(BackgroundJobManagerImpl.formatTimeTag(1581820284000)),
            progress = Data.Builder().build(),
            runAttemptCount = 1,
            generation = 0
        )
    }

    class Manager : Fixture() {

        class SyncObserver<T> : Observer<T> {
            val latch = CountDownLatch(1)
            var value: T? = null
            override fun onChanged(t: T) {
                value = t
                latch.countDown()
            }

            fun getValue(timeout: Long = 3, timeUnit: TimeUnit = TimeUnit.SECONDS): T? {
                val result = latch.await(timeout, timeUnit)
                if (!result) {
                    throw TimeoutException()
                }
                return value
            }
        }

        @Test
        @UiThreadTest
        fun get_all_job_info() {
            // GIVEN
            //      work manager has 2 registered workers
            val platformWorkInfo = listOf(
                buildWorkInfo(0),
                buildWorkInfo(1),
                buildWorkInfo(2)
            )
            val lv = MutableLiveData<List<WorkInfo>>()
            lv.value = platformWorkInfo
            whenever(workManager.getWorkInfosByTagLiveData(eq("*"))).thenReturn(lv)

            // WHEN
            //      job info for all jobs is requested
            val jobs = backgroundJobManager.jobs

            // THEN
            //      live data with job info is returned
            //      live data contains 2 job info instances
            //      job info is sorted by timestamp from newest to oldest
            assertNotNull(jobs)
            val observer = SyncObserver<List<JobInfo>>()
            jobs.observeForever(observer)
            val jobInfo = observer.getValue()
            assertNotNull(jobInfo)
            assertEquals(platformWorkInfo.size, jobInfo?.size)
            jobInfo?.let {
                assertEquals(platformWorkInfo[2].id, it[0].id)
                assertEquals(platformWorkInfo[1].id, it[1].id)
                assertEquals(platformWorkInfo[0].id, it[2].id)
            }
        }

        @Test
        fun cancel_all_jobs() {
            // WHEN
            //      all jobs are cancelled
            backgroundJobManager.cancelAllJobs()

            // THEN
            //      all jobs with * tag are cancelled
            verify(workManager).cancelAllWorkByTag(BackgroundJobManagerImpl.TAG_ALL)
        }
    }

    class ContentObserver : Fixture() {

        private lateinit var request: OneTimeWorkRequest

        @Before
        fun setUp() {
            val requestCaptor: KArgumentCaptor<OneTimeWorkRequest> = argumentCaptor()
            backgroundJobManager.scheduleContentObserverJob()
            verify(workManager).enqueueUniqueWork(
                any(),
                any(),
                requestCaptor.capture()
            )
            assertEquals(1, requestCaptor.allValues.size)
            request = requestCaptor.firstValue
        }

        @Test
        fun job_is_unique_and_replaces_previous_job() {
            verify(workManager).enqueueUniqueWork(
                eq(BackgroundJobManagerImpl.JOB_CONTENT_OBSERVER),
                eq(ExistingWorkPolicy.REPLACE),
                argThat(IsOneTimeWorkRequest())
            )
        }

        @Test
        fun job_request_has_mandatory_tags() {
            assertHasRequiredTags(request.tags, BackgroundJobManagerImpl.JOB_CONTENT_OBSERVER)
        }
    }

    class PeriodicContactsBackup : Fixture() {
        private lateinit var request: PeriodicWorkRequest

        @Before
        fun setUp() {
            val requestCaptor: KArgumentCaptor<PeriodicWorkRequest> = argumentCaptor()
            backgroundJobManager.schedulePeriodicContactsBackup(user)
            verify(workManager).enqueueUniquePeriodicWork(
                any(),
                any(),
                requestCaptor.capture()
            )
            assertEquals(1, requestCaptor.allValues.size)
            request = requestCaptor.firstValue
        }

        @Test
        fun job_is_unique_for_user() {
            verify(workManager).enqueueUniquePeriodicWork(
                eq(BackgroundJobManagerImpl.JOB_PERIODIC_CONTACTS_BACKUP),
                eq(ExistingPeriodicWorkPolicy.KEEP),
                argThat(IsPeriodicWorkRequest())
            )
        }

        @Test
        fun job_request_has_mandatory_tags() {
            assertHasRequiredTags(request.tags, BackgroundJobManagerImpl.JOB_PERIODIC_CONTACTS_BACKUP, user)
        }
    }

    class ImmediateContactsBackup : Fixture() {

        private lateinit var workInfo: MutableLiveData<WorkInfo>
        private lateinit var jobInfo: LiveData<JobInfo?>
        private lateinit var request: OneTimeWorkRequest

        @Before
        fun setUp() {
            val requestCaptor: KArgumentCaptor<OneTimeWorkRequest> = argumentCaptor()
            workInfo = MutableLiveData()
            whenever(workManager.getWorkInfoByIdLiveData(any())).thenReturn(workInfo)
            jobInfo = backgroundJobManager.startImmediateContactsBackup(user)
            verify(workManager).enqueueUniqueWork(
                any(),
                any(),
                requestCaptor.capture()
            )
            assertEquals(1, requestCaptor.allValues.size)
            request = requestCaptor.firstValue
        }

        @Test
        fun job_is_unique_for_user() {
            verify(workManager).enqueueUniqueWork(
                eq(BackgroundJobManagerImpl.JOB_IMMEDIATE_CONTACTS_BACKUP),
                eq(ExistingWorkPolicy.KEEP),
                argThat(IsOneTimeWorkRequest())
            )
        }

        @Test
        fun job_request_has_mandatory_tags() {
            assertHasRequiredTags(request.tags, BackgroundJobManagerImpl.JOB_IMMEDIATE_CONTACTS_BACKUP, user)
        }

        @Test
        @UiThreadTest
        fun job_info_is_obtained_from_work_info() {
            // GIVEN
            //      work info is available
            workInfo.value = buildWorkInfo(0)

            // WHEN
            //      job info has listener
            jobInfo.observeForever {}

            // THEN
            //      converted value is available
            assertNotNull(jobInfo.value)
            assertEquals(workInfo.value?.id, jobInfo.value?.id)
        }
    }

    class ImmediateContactsImport : Fixture() {

        private lateinit var workInfo: MutableLiveData<WorkInfo>
        private lateinit var jobInfo: LiveData<JobInfo?>
        private lateinit var request: OneTimeWorkRequest

        @get:Rule
        var folder: TemporaryFolder = TemporaryFolder()

        @Before
        fun setUp() {
            var selectedContactsFile: File? = null
            try {
                selectedContactsFile = folder.newFile("hashset_cache.txt")
            } catch (_: IOException) {
                Log_OC.e("ImmediateContactsImport", "error creating temporary test file in ")
                fail("hashset_cache cannot be found")
            }

            if (selectedContactsFile == null) {
                fail("hashset_cache cannot be found")
            }

            val requestCaptor: KArgumentCaptor<OneTimeWorkRequest> = argumentCaptor()
            workInfo = MutableLiveData()
            whenever(workManager.getWorkInfoByIdLiveData(any())).thenReturn(workInfo)

            val selectedContacts = intArrayOf(1, 2, 3)
            val contractsAsByteArray = selectedContacts.toByteArray()
            FileUtils.writeByteArrayToFile(selectedContactsFile, contractsAsByteArray)

            jobInfo = backgroundJobManager.startImmediateContactsImport(
                contactsAccountName = "name",
                contactsAccountType = "type",
                vCardFilePath = "/path/to/vcard/file",
                selectedContactsFilePath = selectedContactsFile!!.absolutePath
            )
            verify(workManager).enqueueUniqueWork(
                any(),
                any(),
                requestCaptor.capture()
            )
            assertEquals(1, requestCaptor.allValues.size)
            request = requestCaptor.firstValue
        }

        @Test
        fun job_is_unique() {
            verify(workManager).enqueueUniqueWork(
                eq(BackgroundJobManagerImpl.JOB_IMMEDIATE_CONTACTS_IMPORT),
                eq(ExistingWorkPolicy.KEEP),
                argThat(IsOneTimeWorkRequest())
            )
        }

        @Test
        fun job_request_has_mandatory_tags() {
            assertHasRequiredTags(request.tags, BackgroundJobManagerImpl.JOB_IMMEDIATE_CONTACTS_IMPORT)
        }

        @Test
        @UiThreadTest
        fun job_info_is_obtained_from_work_info() {
            // GIVEN
            //      work info is available
            workInfo.value = buildWorkInfo(0)

            // WHEN
            //      job info has listener
            jobInfo.observeForever {}

            // THEN
            //      converted value is available
            assertNotNull(jobInfo.value)
            assertEquals(workInfo.value?.id, jobInfo.value?.id)
        }
    }

    class FilesUpload : Fixture() {

        @Test
        fun start_files_upload_job_enqueues_batches() {

            val uploadIds = longArrayOf(1, 2, 3, 4, 5)
            whenever(preferences.maxConcurrentUploads).thenReturn(2)

            val continuation: WorkContinuation = mock()
            whenever(workManager.beginUniqueWork(any(), any(), any<List<OneTimeWorkRequest>>())).thenReturn(continuation)


            backgroundJobManager.startFilesUploadJob(user, uploadIds, true)


            val tagCaptor = argumentCaptor<String>()
            val requestsCaptor = argumentCaptor<List<OneTimeWorkRequest>>()

            verify(workManager, timeout(1000)).beginUniqueWork(
                tagCaptor.capture(),
                eq(ExistingWorkPolicy.KEEP),
                requestsCaptor.capture()
            )

            val tag = tagCaptor.firstValue
            assertTrue(tag.startsWith(BackgroundJobManagerImpl.JOB_FILES_UPLOAD + USER_ACCOUNT_NAME + "_"))

            val requests = requestsCaptor.firstValue
            assertEquals(3, requests.size)

            // Check first batch [1, 2]
            val data1 = requests[0].workSpec.input
            assertEquals(true, data1.getBoolean(FileUploadWorker.SHOW_SAME_FILE_ALREADY_EXISTS_NOTIFICATION, false))
            assertEquals(USER_ACCOUNT_NAME, data1.getString(FileUploadWorker.ACCOUNT))
            assertEquals(5, data1.getInt(FileUploadWorker.TOTAL_UPLOAD_SIZE, 0))
            assertTrue(longArrayOf(1, 2).contentEquals(data1.getLongArray(FileUploadWorker.UPLOAD_IDS)!!))
            assertEquals(0, data1.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, -1))

            // Check second batch [3, 4]
            val data2 = requests[1].workSpec.input
            assertTrue(longArrayOf(3, 4).contentEquals(data2.getLongArray(FileUploadWorker.UPLOAD_IDS)!!))
            assertEquals(1, data2.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, -1))

            // Check third batch [5]
            val data3 = requests[2].workSpec.input
            assertTrue(longArrayOf(5).contentEquals(data3.getLongArray(FileUploadWorker.UPLOAD_IDS)!!))
            assertEquals(2, data3.getInt(FileUploadWorker.CURRENT_BATCH_INDEX, -1))

            verify(continuation).enqueue()
        }

        @Test
        fun start_files_upload_job_does_nothing_when_empty() {
            val uploadIds = longArrayOf()
            whenever(preferences.maxConcurrentUploads).thenReturn(2)

            backgroundJobManager.startFilesUploadJob(user, uploadIds, true)

            verify(workManager, timeout(1000).times(0)).beginUniqueWork(any(), any(), any<List<OneTimeWorkRequest>>())
        }
    }

    class Tags {
        @Test
        fun split_tag_key_and_value() {
            // GIVEN
            //      valid tag
            //      tag has colons in value part
            val tag = "${BackgroundJobManagerImpl.TAG_PREFIX_NAME}:value:with:colons and spaces"

            // WHEN
            //      tag is parsed
            val parsedTag = BackgroundJobManagerImpl.parseTag(tag)

            // THEN
            //      key-value pair is returned
            //      key is first
            //      value with colons is second
            assertNotNull(parsedTag)
            assertEquals(BackgroundJobManagerImpl.TAG_PREFIX_NAME, parsedTag?.first)
            assertEquals("value:with:colons and spaces", parsedTag?.second)
        }

        @Test
        fun tags_with_invalid_prefixes_are_rejected() {
            // GIVEN
            //      tag prefix is not on allowed prefixes list
            val tag = "invalidprefix:value"
            BackgroundJobManagerImpl.PREFIXES.forEach {
                assertFalse(tag.startsWith(it))
            }

            // WHEN
            //      tag is parsed
            val parsedTag = BackgroundJobManagerImpl.parseTag(tag)

            // THEN
            //      tag is rejected
            assertNull(parsedTag)
        }

        @Test
        fun strings_without_colon_are_rejected() {
            // GIVEN
            //      strings that are not tags
            val tags = listOf(
                BackgroundJobManagerImpl.TAG_ALL,
                BackgroundJobManagerImpl.TAG_PREFIX_NAME,
                "simplestring",
                ""
            )

            tags.forEach {
                // WHEN
                //      string is parsed
                val parsedTag = BackgroundJobManagerImpl.parseTag(it)

                // THEN
                //      tag is rejected
                assertNull(parsedTag)
            }
        }
    }
}
