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
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.GrantStoragePermissionRule.Companion.grant
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

@Suppress("FunctionNaming")
class SettingsActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.SettingsActivityIT"

    @get:Rule
    var storagePermissionRule: TestRule = grant()

    @Test
    @ScreenshotTest
    fun open() {
        launchActivity<SettingsActivity>().use { scenario ->
            val screenShotName = createName(testClassName + "_" + "open", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showMnemonic_Error() {
        launchActivity<SettingsActivity>().use { scenario ->
            val screenShotName = createName(testClassName + "_" + "showMnemonic_Error", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                sut.handleMnemonicRequest(null)
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
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
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                sut.handleMnemonicRequest(intent)
            }

            Looper.myLooper()?.quitSafely()
            Assert.assertTrue(true)
        }
    }
}
