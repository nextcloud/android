/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class PreviewBitmapScreenshotIT : AbstractIT() {

    companion object {
        private const val PNG_FILE_ASSET = "imageFile.png"
    }

    @Test
    @ScreenshotTest
    fun showBitmap() {
        val pngFile = getFile(PNG_FILE_ASSET)
        val intent = Intent(targetContext, PreviewBitmapActivity::class.java).apply {
            putExtra(
                PreviewBitmapActivity.EXTRA_BITMAP_PATH,
                pngFile.absolutePath
            )
        }
        val sutScenario = ActivityScenario.launch<PreviewBitmapActivity>(intent)
        sutScenario.onActivity { sut ->
            shortSleep()
            onIdleSync {
                screenshot(sut)
            }
        }
    }
}
