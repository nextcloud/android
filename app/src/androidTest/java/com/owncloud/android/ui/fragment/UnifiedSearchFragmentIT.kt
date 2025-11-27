/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import com.owncloud.android.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class UnifiedSearchFragmentIT : AbstractIT() {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun showSearchResult() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = UnifiedSearchFragment.newInstance(null, null, "/")
                    activity.addFragment(sut)

                    sut.onSearchResultChanged(
                        listOf(
                            UnifiedSearchSection(
                                providerID = "files",
                                name = "Files",
                                entries = listOf(
                                    SearchResultEntry(
                                        "thumbnailUrl",
                                        "Test",
                                        "in Files",
                                        "http://localhost/nc/index.php/apps/files/?dir=/Files&scrollto=Test",
                                        "icon",
                                        false
                                    )
                                ),
                                hasMoreResults = false
                            )
                        )
                    )
                    EspressoIdlingResource.decrement()
                    onView(isRoot()).check(matches(isDisplayed()))
                }
            }
        }
    }

    @Test
    fun search() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val sut = UnifiedSearchFragment.newInstance(null, null, "/")
                    val testViewModel = UnifiedSearchViewModel(activity.application)
                    testViewModel.setConnectivityService(activity.connectivityServiceMock)
                    val localRepository = UnifiedSearchFakeRepository()
                    testViewModel.setRepository(localRepository)
                    val ocFile = OCFile("/folder/test1.txt").apply {
                        storagePath = "/sdcard/1.txt"
                        storageManager.saveFile(this)
                    }

                    File(ocFile.storagePath).createNewFile()
                    activity.addFragment(sut)

                    sut.setViewModel(testViewModel)
                    sut.vm.setQuery("test")
                    sut.vm.initialQuery()

                    EspressoIdlingResource.decrement()
                    onView(isRoot()).check(matches(isDisplayed()))
                }
            }
        }
    }
}
