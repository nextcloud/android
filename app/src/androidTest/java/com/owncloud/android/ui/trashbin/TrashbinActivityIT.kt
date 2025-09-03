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
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.extensions.launchAndCapture
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class TrashbinActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.trashbin.TrashbinActivityIT"

    enum class TestCase {
        ERROR,
        EMPTY,
        FILES
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun error() {
        launchActivity<TrashbinActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val trashbinRepository = TrashbinLocalRepository(TestCase.ERROR)
                sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.loadFolder(
                        onComplete = { EspressoIdlingResource.decrement() },
                        onError = { EspressoIdlingResource.decrement() }
                    )
                    val screenShotName = createName(testClassName + "_" + "error", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun files() {
        launchAndCapture<TrashbinActivity>(testClassName, "files", before = { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.FILES)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.loadFolder(
                onComplete = { EspressoIdlingResource.decrement() },
                onError = { EspressoIdlingResource.decrement() }
            )
        })
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun empty() {
        launchAndCapture<TrashbinActivity>(testClassName, "empty", before = { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.loadFolder(
                onComplete = { EspressoIdlingResource.decrement() },
                onError = { EspressoIdlingResource.decrement() }
            )
        })
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun loading() {
        launchAndCapture<TrashbinActivity>(testClassName, "loading", before = { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.showInitialLoading()
        })
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun normalUser() {
        launchAndCapture<TrashbinActivity>(testClassName, "normalUser", before = { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.showUser()
        })
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun differentUser() {
        val temp = Account("differentUser@https://nextcloud.localhost", MainApp.getAccountType(targetContext))

        AccountManager.get(targetContext).apply {
            addAccountExplicitly(temp, "password", null)
            setUserData(temp, AccountUtils.Constants.KEY_OC_BASE_URL, "https://nextcloud.localhost")
            setUserData(temp, AccountUtils.Constants.KEY_USER_ID, "differentUser")
        }

        val intent = Intent(targetContext, TrashbinActivity::class.java).apply {
            putExtra(Intent.EXTRA_USER, "differentUser@https://nextcloud.localhost")
        }

        launchAndCapture<TrashbinActivity>(testClassName, "differentUser", intent = intent, before = { sut ->
            val trashbinRepository = TrashbinLocalRepository(TestCase.EMPTY)
            sut.trashbinPresenter = TrashbinPresenter(trashbinRepository, sut)
            sut.showUser()
        })
    }
}
