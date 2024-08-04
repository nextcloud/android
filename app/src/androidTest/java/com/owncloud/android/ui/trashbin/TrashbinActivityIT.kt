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
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class TrashbinActivityIT : AbstractIT() {
    enum class TestCase {
        ERROR,
        EMPTY,
        FILES
    }

    @get:Rule
    var activityRule = IntentsTestRule(TrashbinActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun error() {
        val sut: TrashbinActivity = activityRule.launchActivity(null)

        val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.loadFolder() }

        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun files() {
        val sut: TrashbinActivity = activityRule.launchActivity(null)

        val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.loadFolder() }

        waitForIdleSync()
        shortSleep()
        shortSleep()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun empty() {
        val sut: TrashbinActivity = activityRule.launchActivity(null)

        val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.loadFolder() }

        shortSleep()
        shortSleep()
        waitForIdleSync()

        screenshot(sut.binding.emptyList.emptyListView)
    }

    @Test
    @ScreenshotTest
    fun loading() {
        val sut: TrashbinActivity = activityRule.launchActivity(null)

        val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.showInitialLoading() }

        shortSleep()

        screenshot(sut.binding.listFragmentLayout)
    }

    @Test
    @ScreenshotTest
    fun normalUser() {
        val sut: TrashbinActivity = activityRule.launchActivity(null)

        val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.showUser() }

        shortSleep()

        screenshot(sut)
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
        val sut: TrashbinActivity = activityRule.launchActivity(intent)

        val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)

        sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)

        sut.runOnUiThread { sut.showUser() }

        shortSleep()

        screenshot(sut)
    }
}
