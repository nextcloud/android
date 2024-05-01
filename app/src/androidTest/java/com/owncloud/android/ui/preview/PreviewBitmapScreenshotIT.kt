/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class PreviewBitmapScreenshotIT : AbstractIT() {

    companion object {
        private const val PNG_FILE_ASSET = "imageFile.png"
    }

    @get:Rule
    val testActivityRule = IntentsTestRule(PreviewBitmapActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showBitmap() {
        val pngFile = getFile(PNG_FILE_ASSET)

        val activity = testActivityRule.launchActivity(
            Intent().putExtra(
                PreviewBitmapActivity.EXTRA_BITMAP_PATH,
                pngFile.absolutePath
            )
        )

        shortSleep()
        waitForIdleSync()

        screenshot(activity)
    }
}
