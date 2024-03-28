/*
 *
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
package com.owncloud.android.ui.trashbin

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class TrashbinActivityIT : AbstractIT() {
    enum class TestCase {
        ERROR, EMPTY, FILES
    }

    private var scenario: ActivityScenario<TrashbinActivity>? = null
    val intent = Intent(ApplicationProvider.getApplicationContext(), TrashbinActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TrashbinActivity>(intent)

    @After
    fun cleanup() {
        scenario?.close()
    }

    @Test
    @ScreenshotTest
    fun error() {
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.loadFolder() }
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun files() {
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.loadFolder() }
                shortSleep()
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun empty() {
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.loadFolder() }
                shortSleep()
                shortSleep()
                screenshot(
                    sut.binding.emptyList.emptyListView,
                    "empty",
                    false
                )
            }
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.showInitialLoading() }
                shortSleep()
                screenshot(sut.binding.listFragmentLayout)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun normalUser() {
        scenario = activityRule.scenario
        scenario?.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.showUser() }
                shortSleep()
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun differentUser() {
        val temp = Account("differentUser@https://nextcloud.localhost", MainApp.getAccountType(targetContext))

        val platformAccountManager = AccountManager.get(targetContext)
        platformAccountManager.addAccountExplicitly(temp, "password", null)
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "https://nextcloud.localhost")
        platformAccountManager.setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "differentUser")

        val intent = Intent(targetContext, TrashbinActivity::class.java)
        intent.putExtra(Intent.EXTRA_USER, "differentUser@https://nextcloud.localhost")

        val sutScenario = ActivityScenario.launch<TrashbinActivity>(intent)
        sutScenario.onActivity { sut ->
            sut.intent = intent
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            onIdleSync {
                sut.runOnUiThread { sut.showUser() }
                shortSleep()
                screenshot(sut)
            }
        }
    }
}
