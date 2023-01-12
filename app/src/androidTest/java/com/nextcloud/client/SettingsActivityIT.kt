/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
