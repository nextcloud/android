/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.trashbin

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.utils.EspressoIdlingResource
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TrashbinActivityIT : AbstractIT() {
    enum class TestCase {
        ERROR,
        EMPTY,
        FILES
    }

    private var scenario: ActivityScenario<TrashbinActivity>? = null
    val intent = Intent(ApplicationProvider.getApplicationContext(), TrashbinActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TrashbinActivity>(intent)

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
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
                screenshot(sut)
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
                screenshot(sut)
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
                screenshot(sut)
            }
        }
    }
}
