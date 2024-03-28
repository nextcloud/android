/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
