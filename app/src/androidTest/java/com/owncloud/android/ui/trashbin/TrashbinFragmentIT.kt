/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.trashbin

import android.accounts.Account
import android.accounts.AccountManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class TrashbinFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.trashbin.TrashbinFragmentIT"

    enum class TestCase {
        ERROR,
        EMPTY,
        FILES
    }

    @Suppress("ReturnCount")
    private fun findFragment(sut: NavigatorActivity): TrashbinFragment? {
        val allFragments = sut.supportFragmentManager.fragments
        for (f in allFragments) {
            if (f is TrashbinFragment) return f
            val child = f.childFragmentManager.fragments.filterIsInstance<TrashbinFragment>().firstOrNull()
            if (child != null) return child
        }
        return null
    }

    private fun launchFragment(name: String, block: TrashbinFragment.() -> Unit) {
        val intent = NavigatorActivity.intent(targetContext, NavigatorScreen.Trashbin)

        ActivityScenario.launch<NavigatorActivity>(intent).use { scenario ->
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                val fragment = findFragment(sut)
                    ?: throw IllegalStateException("TrashbinFragment not found in NavigatorActivity!")
                fragment.block()
            }

            onView(isRoot()).check(matches(isDisplayed()))

            val screenShotName = createName(testClassName + "_" + name, "")
            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        launchFragment("error") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            loadFolder(
                onComplete = { },
                onError = { }
            )
        }
    }

    @Test
    @ScreenshotTest
    fun files() {
        launchFragment("files") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            loadFolder(
                onComplete = { },
                onError = { }
            )
        }
    }

    @Test
    @ScreenshotTest
    fun empty() {
        launchFragment("empty") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            loadFolder(
                onComplete = { },
                onError = { }
            )
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        launchFragment("loading") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            showInitialLoading()
        }
    }

    @Test
    @ScreenshotTest
    fun normalUser() {
        launchFragment("normalUser") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            showUser()
        }
    }

    @Test
    @ScreenshotTest
    fun differentUser() {
        val temp = Account("differentUser@https://nextcloud.localhost", MainApp.getAccountType(targetContext))

        AccountManager.get(targetContext).apply {
            addAccountExplicitly(temp, "password", null)
            setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "https://nextcloud.localhost")
            setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "differentUser")
        }

        launchFragment("differentUser") {
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            trashbinPresenter = TrashbinPresenter(trashbinRepository, this)
            showUser()
        }
    }
}
