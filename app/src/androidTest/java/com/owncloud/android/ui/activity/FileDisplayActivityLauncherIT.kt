/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileDisplayActivityLauncherIT {

    @Test
    fun tappingTheHomeScreenIconOpensFileDisplayActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(context.packageName)

        val launcher = context.packageManager.resolveActivity(launchIntent, 0)

        assertEquals(FileDisplayActivity::class.java.name, launcher?.activityInfo?.name)
    }
}
