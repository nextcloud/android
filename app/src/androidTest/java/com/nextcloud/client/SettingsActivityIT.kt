/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.content.Intent
import android.os.Looper
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.GrantStoragePermissionRule.Companion.grant
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@Suppress("FunctionNaming")
class SettingsActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.SettingsActivityIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @get:Rule
    var storagePermissionRule: TestRule = grant()

    @Test
    @UiThread
    @ScreenshotTest
    fun open() {
        launchActivity<SettingsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "open", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showMnemonic_Error() {
        launchActivity<SettingsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    sut.handleMnemonicRequest(null)
                    val screenShotName = createName(testClassName + "_" + "showMnemonic_Error", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThread
    fun showMnemonic() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val intent = Intent().apply {
            putExtra(RequestCredentialsActivity.KEY_CHECK_RESULT, RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE)
        }

        ArbitraryDataProviderImpl(targetContext).run {
            storeOrUpdateKeyValue(user.accountName, EncryptionUtils.MNEMONIC, "Secret mnemonic")
        }

        launchActivity<SettingsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    sut.handleMnemonicRequest(intent)
                    onView(isRoot()).check(matches(isDisplayed()))
                    Looper.myLooper()?.quitSafely()
                    Assert.assertTrue(true)
                }
            }
        }
    }
}
