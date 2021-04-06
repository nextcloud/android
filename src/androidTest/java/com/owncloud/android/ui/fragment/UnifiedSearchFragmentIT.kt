/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchViewModel
import org.junit.Rule
import org.junit.Test
import java.io.File

class UnifiedSearchFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun showSearchResult() {
        val activity = testActivityRule.launchActivity(null)
        val sut = UnifiedSearchFragment.newInstance(null)

        activity.addFragment(sut)

        shortSleep()

        UiThreadStatement.runOnUiThread {
            sut.onSearchResultChanged(
                mutableListOf(
                    SearchResult(
                        "Files",
                        false,
                        listOf(
                            SearchResultEntry(
                                "thumbnailUrl",
                                "Test",
                                "in /Files/",
                                "resourceUrl",
                                "icon",
                                false
                            )
                        )
                    )
                )
            )
        }

        longSleep()
    }

    @Test
    fun search() {
        val activity = testActivityRule.launchActivity(null)
        val sut = UnifiedSearchFragment.newInstance(null)
        val testViewModel = UnifiedSearchViewModel()
        val localRepository = UnifiedSearchLocalRepository()
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
            sut.vm.startLoading("test")
        }

        longSleep()
    }
}
