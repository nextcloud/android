/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.app.Activity
import android.content.Intent
import android.os.Looper
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.GrantStoragePermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@Suppress("FunctionNaming")
class SettingsActivityIT : AbstractIT() {
    @get:Rule
    val activityRule = IntentsTestRule(
        SettingsActivity::class.java,
        true,
        false
    )

    @get:Rule
    val permissionRule = GrantStoragePermissionRule.grant()

    @Test
    @ScreenshotTest
    fun open() {
        val sut: Activity = activityRule.launchActivity(null)
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showMnemonic_Error() {
        val sut = activityRule.launchActivity(null)
        sut.handleMnemonicRequest(null)
        shortSleep()
        waitForIdleSync()
        screenshot(sut)
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
        val sut = activityRule.launchActivity(null)
        sut.runOnUiThread {
            sut.handleMnemonicRequest(intent)
        }

        Looper.myLooper()?.quitSafely()
        Assert.assertTrue(true) // if we reach this, everything is ok
    }
}
