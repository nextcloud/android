/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client

import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@Suppress("FunctionNaming")
class SettingsActivityIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<SettingsActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), SettingsActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<SettingsActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @get:Rule
    val permissionRule = GrantStoragePermissionRule.grant()

    @Test
    @ScreenshotTest
    fun open() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            screenshot(sut)
        }
    }

    @Test
    @ScreenshotTest
    fun showMnemonic_Error() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.handleMnemonicRequest(null)
            shortSleep()
            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    fun showMnemonic() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val intent = Intent()
        intent.putExtra(RequestCredentialsActivity.KEY_CHECK_RESULT, RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE)
        val arbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        arbitraryDataProvider.storeOrUpdateKeyValue(user.accountName, EncryptionUtils.MNEMONIC, "Secret mnemonic")

        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            sut.runOnUiThread {
                sut.handleMnemonicRequest(intent)
            }

            Looper.myLooper()?.quitSafely()
            Assert.assertTrue(true) // if we reach this, everything is ok
        }
    }
}
