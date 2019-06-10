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
package com.nextcloud.client.etm

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    TestEtmViewModel.MainPage::class,
    TestEtmViewModel.PreferencesPage::class
)
class TestEtmViewModel {

    internal abstract class Base {

        @get:Rule
        val rule = InstantTaskExecutorRule()

        protected lateinit var sharedPreferences: SharedPreferences
        protected lateinit var vm: EtmViewModel

        @Before
        fun setUpBase() {
            sharedPreferences = mock()
            vm = EtmViewModel(sharedPreferences)
        }
    }

    internal class MainPage : Base() {

        @Test
        fun `current page is not set`() {
            // GIVEN
            //      main page is displayed
            // THEN
            //      current page is null
            assertNull(vm.currentPage.value)
        }

        @Test
        fun `back key is not handled`() {
            // GIVEN
            //      main page is displayed
            // WHEN
            //      back key is pressed
            val handled = vm.onBackPressed()

            // THEN
            //      is not handled
            assertFalse(handled)
        }

        @Test
        fun `page is selected`() {
            val observer: Observer<EtmMenuEntry?> = mock()
            val selectedPageIndex = 0
            val expectedPage = vm.pages[selectedPageIndex]

            // GIVEN
            //      main page is displayed
            //      current page observer is registered
            vm.currentPage.observeForever(observer)
            reset(observer)

            // WHEN
            //      page is selected
            vm.onPageSelected(selectedPageIndex)

            // THEN
            //      current page is set
            //      page observer is called once with selected entry
            assertNotNull(vm.currentPage.value)
            verify(observer, times(1)).onChanged(same(expectedPage))
        }

        @Test
        fun `out of range index is ignored`() {
            val maxIndex = vm.pages.size
            // GIVEN
            //      observer is registered
            val observer: Observer<EtmMenuEntry?> = mock()
            vm.currentPage.observeForever(observer)
            reset(observer)

            // WHEN
            //      out of range page index is selected
            vm.onPageSelected(maxIndex + 1)

            // THEN
            //      nothing happens
            verify(observer, never()).onChanged(anyOrNull())
            assertNull(vm.currentPage.value)
        }
    }

    internal class PreferencesPage : Base() {

        @Before
        fun setUp() {
            vm.onPageSelected(0)
        }

        @Test
        fun `back goes back to main page`() {
            val observer: Observer<EtmMenuEntry?> = mock()

            // GIVEN
            //      a page is selected
            //      page observer is registered
            assertNotNull(vm.currentPage.value)
            vm.currentPage.observeForever(observer)

            // WHEN
            //      back is pressed
            val handled = vm.onBackPressed()

            // THEN
            //      back press is handled
            //      observer is called with null page
            assertTrue(handled)
            verify(observer).onChanged(eq(null))
        }

        @Test
        fun `back is handled only once`() {
            // GIVEN
            //      a page is selected
            assertNotNull(vm.currentPage.value)

            // WHEN
            //      back is pressed twice
            val first = vm.onBackPressed()
            val second = vm.onBackPressed()

            // THEN
            //      back is handled only once
            assertTrue(first)
            assertFalse(second)
        }

        @Test
        fun `preferences are loaded from shared preferences`() {
            // GIVEN
            //      shared preferences contain values of different types
            val preferenceValues: Map<String, Any> = mapOf(
                "key1" to 1,
                "key2" to "value2",
                "key3" to false
            )
            whenever(sharedPreferences.all).thenReturn(preferenceValues)

            // WHEN
            //      vm preferences are read
            val prefs = vm.preferences

            // THEN
            //      all preferences are converted to strings
            assertEquals(preferenceValues.size, prefs.size)
            assertEquals("1", prefs["key1"])
            assertEquals("value2", prefs["key2"])
            assertEquals("false", prefs["key3"])
        }
    }
}
