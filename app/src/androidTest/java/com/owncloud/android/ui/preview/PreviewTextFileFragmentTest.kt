/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.preview

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.MimeTypeUtil
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PreviewTextFileFragmentTest : AbstractIT() {
    @Rule
    var activityRule = IntentsTestRule(
        FileDisplayActivity::class.java,
        true,
        false
    )

    @Test // @ScreenshotTest // todo run without real server
    @Throws(IOException::class)
    fun displaySimpleTextFile() {
        val sut = activityRule.launchActivity(null)
        shortSleep()
        val file = getDummyFile("nonEmpty.txt")
        val test = OCFile("/text.md")
        test.mimeType = MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN
        test.storagePath = file.absolutePath
        sut.startTextPreview(test, false)
        shortSleep()
        screenshot(sut)
    }

    @Test // @ScreenshotTest // todo run without real server
    @Throws(IOException::class)
    fun displayJavaSnippetFile() {
        val sut = activityRule.launchActivity(null)
        shortSleep()
        val file = getFile("java.md")
        val test = OCFile("/java.md")
        test.mimeType = MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN
        test.storagePath = file.absolutePath
        sut.startTextPreview(test, false)
        shortSleep()
        screenshot(sut)
    }
}