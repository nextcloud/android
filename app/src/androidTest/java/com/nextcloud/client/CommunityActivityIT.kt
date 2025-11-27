/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.GrantStoragePermissionRule.Companion.grant
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.activity.CommunityActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class CommunityActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.CommunityActivityIT"

    @get:Rule
    var storagePermissionRule: TestRule = grant()

    @Test
    @ScreenshotTest
    fun open() {
        launchActivity<CommunityActivity>().use { scenario ->
            val screenShotName = createName(testClassName + "_" + "open", "")
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }
}
