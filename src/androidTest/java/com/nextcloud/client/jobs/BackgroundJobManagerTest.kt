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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.annotation.UiThreadTest
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.ArgumentMatcher
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
        internal lateinit var backgroundJobManager: BackgroundJobManagerImpl

        @Before
        fun setUpFixture() {
            user = mock()
            whenever(user.accountName).thenReturn(USER_ACCOUNT_NAME)
            workManager = mock()
            clock = mock()
            whenever(clock.currentTime).thenReturn(TIMESTAMP)
            whenever(clock.currentDate).thenReturn(Date(TIMESTAMP))
            backgroundJobManager = BackgroundJobManagerImpl(workManager, clock)
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
            UUID.randomUUID(),
            WorkInfo.State.RUNNING,
            Data.Builder().build(),
            listOf(BackgroundJobManagerImpl.formatTimeTag(1581820284000)),
            Data.Builder().build(),
            1
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

        @Before
        fun setUp() {
            val requestCaptor: KArgumentCaptor<OneTimeWorkRequest> = argumentCaptor()
            workInfo = MutableLiveData()
            whenever(workManager.getWorkInfoByIdLiveData(any())).thenReturn(workInfo)
            jobInfo = backgroundJobManager.startImmediateContactsImport(
                contactsAccountName = "name",
                contactsAccountType = "type",
                vCardFilePath = "/path/to/vcard/file",
                selectedContacts = intArrayOf(1, 2, 3)
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
