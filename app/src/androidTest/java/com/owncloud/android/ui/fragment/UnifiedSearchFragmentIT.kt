/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.File

class UnifiedSearchFragmentIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun showSearchResult() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = UnifiedSearchFragment.newInstance(null, null)

            activity.addFragment(sut)

            shortSleep()

            onIdleSync {
                UiThreadStatement.runOnUiThread {
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
                shortSleep()
            }
        }
    }

    @Test
    fun search() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = UnifiedSearchFragment.newInstance(null, null)
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

            shortSleep()

            UiThreadStatement.runOnUiThread {
                sut.setViewModel(testViewModel)
                sut.vm.setQuery("test")
                sut.vm.initialQuery()
            }
            shortSleep()
        }
    }
}
