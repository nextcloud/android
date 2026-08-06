/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.view.View
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActivityIT : AbstractIT() {

    @Test
    fun testSplashScreenWithEmptyTitlesShouldHideTitles() {
        launchActivity<LauncherActivity>().onActivity { activity ->
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.ivSplash).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.splashScreenBold).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.splashScreenNormal).visibility)
        }
    }

    @Test
    fun testSplashScreenWithTitlesShouldShowTitles() {
        launchActivity<LauncherActivity>().onActivity { activity ->
            activity.setSplashTitles("Example", "Cloud")

            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.ivSplash).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.splashScreenBold).visibility)
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.splashScreenNormal).visibility)
        }
    }
}
