/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class UserInfoActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.UserInfoActivityIT"

    @Test
    @ScreenshotTest
    fun fullUserInfoDetail() {
        val intent = Intent(targetContext, UserInfoActivity::class.java).apply {
            putExtra(UserInfoActivity.KEY_ACCOUNT, user)

            val userInfo = UserInfo(
                "test",
                true,
                "Firstname Familyname",
                "oss@rocks.com",
                "+49 7613 672 255",
                "Awesome Place Av.",
                "https://www.nextcloud.com",
                "nextclouders",
                null,
                null
            )
            putExtra(UserInfoActivity.KEY_USER_DATA, userInfo)
        }

        launchActivity<UserInfoActivity>(intent).use { scenario ->
            val screenShotName = createName(testClassName + "_" + "fullUserInfoDetail", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }
}
