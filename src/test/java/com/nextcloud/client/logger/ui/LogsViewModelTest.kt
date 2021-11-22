/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.logger.ui

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ManualAsyncRunner
import com.nextcloud.client.logger.Level
import com.nextcloud.client.logger.LogEntry
import com.nextcloud.client.logger.LogsRepository
import com.nextcloud.client.logger.OnLogsLoaded
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.MockitoAnnotations
import java.util.Date

@RunWith(Suite::class)
@Suite.SuiteClasses(
    LogsViewModelTest.Loading::class,
    LogsViewModelTest.Filtering::class
)
class LogsViewModelTest {

    private companion object {
        val TEST_LOG_ENTRIES = listOf(
            LogEntry(Date(), Level.DEBUG, "test", "entry 1"),
            LogEntry(Date(), Level.DEBUG, "test", "entry 2"),
            LogEntry(Date(), Level.DEBUG, "test", "entry 3")
        )
        val TEST_LOG_SIZE_KILOBYTES = 42L
        val TEST_LOG_SIZE_BYTES = TEST_LOG_SIZE_KILOBYTES * 1024L
        const val TOTAL_ENTRY_COUNT = 3
        const val QUERY_TIME = 4
    }

    class TestLogRepository : LogsRepository {
        var loadRequestCount = 0
        var onLoadedCallback: OnLogsLoaded? = null

        override val lostEntries: Boolean = false
        override fun load(onLoaded: OnLogsLoaded) { this.onLoadedCallback = onLoaded; loadRequestCount++ }
        override fun deleteAll() { /* no implementation neeeded */
        }
    }

    abstract class Fixture {

        protected lateinit var context: Context
        protected lateinit var clock: Clock
        protected lateinit var repository: TestLogRepository
        protected lateinit var runner: ManualAsyncRunner
        protected lateinit var vm: LogsViewModel

        @get:Rule
        val rule = InstantTaskExecutorRule()

        @Before
        fun setUpFixture() {
            MockitoAnnotations.initMocks(this)
            context = mock()
            clock = mock()
            repository = TestLogRepository()
            runner = ManualAsyncRunner()
            vm = LogsViewModel(context, clock, runner, repository)
            whenever(context.getString(any(), any())).thenAnswer {
                "${it.arguments}"
            }
        }
    }

    class Loading : Fixture() {

        @Test
        fun `all observable properties have initial values`() {
            assertNotNull(vm.isLoading)
            assertNotNull(vm.size)
            assertNotNull(vm.entries)
            assertNotNull(vm.status)
        }

        @Test
        fun `load logs entries from repository`() {
            // GIVEN
            //      entries are not loaded
            assertEquals(0, vm.entries.value!!.size)
            assertEquals(false, vm.isLoading.value)

            // WHEN
            //      load is initiated
            vm.load()

            // THEN
            //      loading status is true
            //      repository request is posted
            assertTrue(vm.isLoading.value!!)
            assertNotNull(repository.onLoadedCallback)
        }

        @Test
        fun `on logs loaded`() {
            // GIVEN
            //      logs are being loaded
            vm.load()
            assertNotNull(repository.onLoadedCallback)
            assertTrue(vm.isLoading.value!!)

            // WHEN
            //      logs loading finishes
            repository.onLoadedCallback?.invoke(TEST_LOG_ENTRIES, TEST_LOG_SIZE_BYTES)

            // THEN
            //      logs are displayed
            //      logs size is displyed
            //      status is displayed
            assertFalse(vm.isLoading.value!!)
            assertSame(vm.entries.value, TEST_LOG_ENTRIES)
            assertNotNull(vm.status.value)
        }

        @Test
        fun `cannot start loading when loading is in progress`() {
            // GIVEN
            //      logs loading is started
            vm.load()
            assertEquals(1, repository.loadRequestCount)
            assertTrue(vm.isLoading.value!!)

            // WHEN
            //      load is requested
            repository.onLoadedCallback = null
            vm.load()

            // THEN
            //      request is ignored
            assertNull(repository.onLoadedCallback)
            assertEquals(1, repository.loadRequestCount)
        }
    }

    class Filtering : Fixture() {

        @Before
        fun setUp() {
            vm.load()
            repository.onLoadedCallback?.invoke(TEST_LOG_ENTRIES, TEST_LOG_SIZE_BYTES)
            assertFalse(vm.isLoading.value!!)
            assertEquals(TEST_LOG_ENTRIES.size, vm.entries.value?.size)
        }

        @Test
        fun `filtering  cannot be started when loading`() {
            // GIVEN
            //      loading is in progress
            vm.load()
            assertTrue(vm.isLoading.value!!)

            // WHEN
            //      filtering is requested
            vm.filter("some pattern")

            // THEN
            //      filtering is not enqueued
            assertTrue(runner.isEmpty)
        }

        @Test
        fun `filtering task is started`() {
            // GIVEN
            //      logs are loaded
            assertEquals(TEST_LOG_ENTRIES.size, vm.entries.value?.size)

            // WHEN
            //      logs filtering is not running
            //      logs filtering is requested
            assertTrue(runner.isEmpty)
            vm.filter(TEST_LOG_ENTRIES[0].message)

            // THEN
            //      filter request is enqueued
            assertEquals(1, runner.size)
        }

        @Test
        fun `filtered logs are displayed`() {
            var statusArgs: Array<Any> = emptyArray()
            whenever(context.getString(any(), any())).thenAnswer {
                statusArgs = it.arguments
                "${it.arguments}"
            }
            // GIVEN
            //      filtering is in progress
            val pattern = TEST_LOG_ENTRIES[0].message
            vm.filter(pattern)

            // WHEN
            //      filtering finishes
            assertEquals(1, runner.runAll())

            // THEN
            //      vm displays filtered results
            //      vm displays status
            assertNotNull(vm.entries.value)
            assertEquals(1, vm.entries.value?.size)
            val filteredEntry = vm.entries.value?.get(0)!!
            assertTrue(filteredEntry.message.contains(pattern))

            assertEquals("Status should contain size in kB", TEST_LOG_SIZE_KILOBYTES, statusArgs[1])
            assertEquals("Status should show matched entries count", vm.entries.value?.size, statusArgs[2])
            assertEquals(
                "Status should contain total entries count",
                TEST_LOG_ENTRIES.size,
                statusArgs[TOTAL_ENTRY_COUNT]
            )
            assertTrue("Status should contain query time in ms", statusArgs[QUERY_TIME] is Long)
        }
    }
}
