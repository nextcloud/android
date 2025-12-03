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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import org.junit.Test
import java.io.File

class UnifiedSearchFragmentIT : AbstractIT() {

    @Test
    fun showSearchResult() {
        launchActivity<TestActivity>().use { scenario ->

            scenario.onActivity { activity ->
                val sut = UnifiedSearchFragment.newInstance(null, null, "/")
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
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
            }

            onView(isRoot()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun search() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
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
                activity.supportFragmentManager.executePendingTransactions()
                sut.setViewModel(testViewModel)
                sut.vm.setQuery("test")
                sut.vm.initialQuery()
            }

            onView(isRoot()).check(matches(isDisplayed()))
        }
    }
}
