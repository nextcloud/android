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
package com.owncloud.android.ui.activities

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.base.MainThread
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.core.ManualAsyncRunner
import com.nextcloud.client.network.ClientFactory
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.activities.model.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.ArgumentMatcher

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ActivitiesViewModelTest.Initialization::class,
    ActivitiesViewModelTest.StartLoading::class,
    ActivitiesViewModelTest.LoadMore::class,
    ActivitiesViewModelTest.OpenFile::class
)
class ActivitiesViewModelTest {

    abstract class Base {
        protected lateinit var context: Context
        protected lateinit var contentResolver: ContentResolver
        protected lateinit var user: User
        protected lateinit var currentUser: CurrentAccountProvider
        protected lateinit var runner: ManualAsyncRunner
        protected lateinit var clientFactory: ClientFactory
        protected lateinit var client: OwnCloudClient
        protected lateinit var resources: Resources
        protected lateinit var vm: ActivitiesViewModel

        private val userMatcher = object : ArgumentMatcher<User> {
            override fun matches(argument: User?): Boolean = true
        }

        @Before
        fun setUpBase() {
            context = mock()
            contentResolver = mock()
            user = mock()
            whenever(user.toPlatformAccount()).thenReturn(mock())
            currentUser = mock()
            whenever(currentUser.user).thenReturn(user)
            runner = ManualAsyncRunner()
            clientFactory = mock()
            client = mock()
            resources = ApplicationProvider.getApplicationContext<MainApp>().resources
            whenever(clientFactory.create(argThat(userMatcher))).thenReturn(client)
            vm = ActivitiesViewModel(
                context,
                contentResolver,
                currentUser,
                runner,
                clientFactory,
                resources
            )
        }
    }

    class Initialization : Base() {

        @Test
        @MainThread
        fun live_data_is_not_null() {
            // GIVEN
            //      vm is created

            // THEN
            //      all live data has default non-null values
            assertNotNull(vm.isLoading.value)
            assertNotNull(vm.activities.value)
            assertNotNull(vm.error.value)
        }
    }

    class StartLoading : Base() {

        @Test
        @UiThreadTest
        fun start_loading() {
            // GIVEN
            //      vm is initialized
            //      vm has no previous data

            // WHEN
            //      loading is started first time
            vm.startLoading()

            // THEN
            //      is loading flag is set
            assertTrue(vm.isLoading.value ?: false)
        }

        @Test
        @UiThreadTest
        fun start_loading_is_idempotent() {
            // GIVEN
            //      vm is initialized
            //      vm is started
            vm.startLoading()

            // WHEN
            //      loading is started second time
            vm.startLoading()

            // THEN
            //      loading task is started once
            assertEquals(1, runner.size)
        }
    }

    class LoadMore : Base() {

        @Test
        @UiThreadTest
        fun activities_are_loaded() {
            // GIVEN
            //      vm is started
            vm.startLoading()

            // WHEN
            //      results are delivered
            val activities = listOf(Activity(), Activity())
            val result = GetActivityListTask.Result(true, activities)
            runner.runOneSuccessful(result)

            // THEN
            //      result are published
            assertEquals(activities.size, vm.activities.value?.size)
        }

        @Test
        @UiThreadTest
        fun loading_more_activities_appends_to_existing_result() {
            // GIVEN
            //      vm loaded some data
            vm.startLoading()
            val initialActivities = listOf(Activity())
            runner.runOneSuccessful(GetActivityListTask.Result(true, initialActivities))
            assertEquals(1, vm.activities.value?.size ?: 0)

            // WHEN
            //      new batch of activities is loaded
            vm.loadMore()
            val moreActivities = listOf(Activity(), Activity())
            runner.runOneSuccessful(GetActivityListTask.Result(true, moreActivities))

            // THEN
            //      activities are appended to existing result
            assertEquals(
                initialActivities.size + moreActivities.size,
                vm.activities.value?.size ?: 0
            )
        }

        @Test
        @UiThreadTest
        fun cannot_load_more_while_initial_loading_in_progress() {
            // GIVEN
            //      vm runs initial loading
            vm.startLoading()
            assertEquals(1, runner.size)

            // WHEN
            //      load more is requested
            vm.loadMore()

            // THEN
            //      load more is not started
            assertEquals(1, runner.size)
        }

        @Test
        @UiThreadTest
        fun loading_fails() {
            // GIVEN
            //      vm is loading
            //      vm has no error
            vm.startLoading()
            assertEquals(1, runner.size)
            assertEquals("", vm.error.value)

            // WHEN
            //      loading fails
            runner.runOneSuccessful(GetActivityListTask.Result(false))

            // THEN
            //      error is flagged
            assertNotNull(vm.error.value)
        }
    }

    class OpenFile : Base() {

        @Before
        @UiThreadTest
        fun setUp() {
            vm.openFile("file:///path/to/a/file")
        }

        @Test
        @UiThreadTest
        fun open_file_can_be_started_once() {
            // GIVEN
            //      file is being opened
            assertEquals(1, runner.size)
            assertTrue(vm.isLoading.value ?: false)

            // WHEN
            //      file is opened again
            vm.openFile("/some/file")

            // THEN
            //      new task is not enqueued
            assertEquals(1, runner.size)
        }

        @Test
        @UiThreadTest
        fun open_file_starts_loading_task() {
            // GIVEN
            //      file is being opened

            // THEN
            //      loading is started
            //      loading task is enqueued
            assertTrue(vm.isLoading.value ?: false)
            assertEquals(1, runner.size)
        }

        @Test
        @UiThreadTest
        fun open_file_task_succeeds() {
            // WHEN
            //      task finishes successfully
            val file = OCFile("/path/to/file")
            runner.runOneSuccessful(GetRemoteFileTask.Result(true, file))

            // THEN
            //      downloaded file is propagated
            assertSame(file, vm.file.value)
        }

        @Test
        @UiThreadTest
        fun open_file_task_fails() {
            // GIVEN
            //      file is being opened
            //      file attribute has observer
            var observerCalled = false
            vm.file.observeForever { observerCalled = true }
            assertFalse(observerCalled)

            // WHEN
            //      task fails
            runner.runOneSuccessful(GetRemoteFileTask.Result(success = false))

            // THEN
            //      downloaded file is propagated
            assertTrue(observerCalled)
        }
    }
}
