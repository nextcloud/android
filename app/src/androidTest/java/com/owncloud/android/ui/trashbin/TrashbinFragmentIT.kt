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
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class TrashbinFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.trashbin.TrashbinFragmentIT"

    enum class TestCase {
        ERROR,
        EMPTY,
        FILES
    }

    @Test
    @ScreenshotTest
    fun error() {
        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.loadFolder(
                    onComplete = { },
                    onError = { }
                )
            }

            val screenShotName = createName(testClassName + "_" + "error", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun files() {
        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.loadFolder(
                    onComplete = { },
                    onError = { }
                )
            }

            val screenShotName = createName(testClassName + "_" + "files", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun empty() {
        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.loadFolder(
                    onComplete = { },
                    onError = { }
                )
            }

            val screenShotName = createName(testClassName + "_" + "empty", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun loading() {
        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.showInitialLoading()
            }

            val screenShotName = createName(testClassName + "_" + "loading", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun normalUser() {
        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.showUser()
            }

            val screenShotName = createName(testClassName + "_" + "normalUser", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
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

        val intent = Intent(targetContext, TrashbinFragment::class.java).apply {
            putExtra(Intent.EXTRA_USER, "differentUser@https://nextcloud.localhost")
        }

        launchActivity<TrashbinFragment>().use { scenario ->
            var sut: TrashbinFragment? = null
            scenario.onActivity { activity ->
                sut = activity
                val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                sut.showUser()
            }

            val screenShotName = createName(testClassName + "_" + "differentUser", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(sut, screenShotName)
        }
    }
}
