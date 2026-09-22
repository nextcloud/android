/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActivityIT : AbstractIT() {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun testSplashScreenWithEmptyTitlesShouldHideTitles() {
        val activity = launchLauncherActivity()

        assertEquals(View.GONE, activity.findViewById<View>(R.id.splashScreenBold).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.splashScreenNormal).visibility)
    }

    @Test
    fun testSplashScreenWithTitlesShouldShowTitles() {
        val activity = launchLauncherActivity()

        instrumentation.runOnMainSync { activity.setSplashTitles("Example", "Cloud") }

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.splashScreenBold).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.splashScreenNormal).visibility)
    }

    @Test
    fun testSplashScreenWithEmptyTitlesShouldNotDelayNextScreen() {
        val activity = launchLauncherActivity()

        instrumentation.waitForIdleSync()

        assertTrue(activity.isFinishing)
    }

    private fun launchLauncherActivity(): LauncherActivity {
        val intent = Intent(targetContext, LauncherActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return instrumentation.startActivitySync(intent) as LauncherActivity
    }
}
