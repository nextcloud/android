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
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class PreviewImageFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun corruptImage() {
        val activity = testActivityRule.launchActivity(null)

        val ocFile = OCFile("/test.png")
        val sut = PreviewImageFragment.newInstance(ocFile, true, false)

        activity.addFragment(sut)

        shortSleep()

        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun validImage() {
        val activity = testActivityRule.launchActivity(null)

        val ocFile = OCFile("/test.png")
        ocFile.storagePath = getFile("imageFile.png").absolutePath

        val sut = PreviewImageFragment.newInstance(ocFile, true, false)

        activity.addFragment(sut)

        shortSleep()

        screenshot(activity)
    }
}
