/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

    private lateinit var scenario: ActivityScenario<TrashbinActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TrashbinActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TrashbinActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun error() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.runOnUiThread { sut.loadFolder() }
            shortSleep()
            screenshot(sut)
        }
    }

    @Test
    @ScreenshotTest
    fun files() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)

            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

            sut.runOnUiThread { sut.loadFolder() }

            onIdleSync {
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
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)

            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

            sut.runOnUiThread { sut.loadFolder() }

            shortSleep()
            shortSleep()
            onIdleSync {
                screenshot(sut.binding.emptyList.emptyListView)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.runOnUiThread { sut.showInitialLoading() }
            shortSleep()
            screenshot(sut.binding.listFragmentLayout)
        }
    }

    @Test
    @ScreenshotTest
    fun normalUser() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.runOnUiThread { sut.showUser() }
            shortSleep()
            screenshot(sut)
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

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_USER, "differentUser@https://nextcloud.localhost")
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.runOnUiThread { sut.showUser() }
            shortSleep()
            screenshot(sut)
        }
    }
}
