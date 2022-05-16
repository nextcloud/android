/*
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

package com.owncloud.android.ui.fragment

import android.graphics.BitmapFactory
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.TextDrawable
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class AvatarIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showAvatars() {
        val avatarRadius = targetContext.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
        val width = DisplayUtils.convertDpToPixel(2 * avatarRadius, targetContext)
        val sut = testActivityRule.launchActivity(null)
        val fragment = AvatarTestFragment()

        sut.addFragment(fragment)

        runOnUiThread {
            fragment.addAvatar("Admin", avatarRadius, width, targetContext)
            fragment.addAvatar("Test Server Admin", avatarRadius, width, targetContext)
            fragment.addAvatar("Cormier Paulette", avatarRadius, width, targetContext)
            fragment.addAvatar("winston brent", avatarRadius, width, targetContext)
            fragment.addAvatar("Baker James Lorena", avatarRadius, width, targetContext)
            fragment.addAvatar("Baker  James   Lorena", avatarRadius, width, targetContext)
            fragment.addAvatar("email@nextcloud.localhost", avatarRadius, width, targetContext)
        }

        shortSleep()
        waitForIdleSync()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showAvatarsWithStatus() {
        val avatarRadius = targetContext.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
        val width = DisplayUtils.convertDpToPixel(2 * avatarRadius, targetContext)
        val sut = testActivityRule.launchActivity(null)
        val fragment = AvatarTestFragment()

        val paulette = BitmapFactory.decodeFile(getFile("paulette.jpg").absolutePath)
        val christine = BitmapFactory.decodeFile(getFile("christine.jpg").absolutePath)
        val textBitmap = BitmapUtils.drawableToBitmap(TextDrawable.createNamedAvatar("Admin", avatarRadius))

        sut.addFragment(fragment)

        runOnUiThread {
            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(paulette, StatusType.ONLINE, "😘", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "☁️", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "🌴️", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(paulette, StatusType.DND, "", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(christine, StatusType.AWAY, "", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(paulette, StatusType.OFFLINE, "", targetContext),
                width * 2,
                1,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "😘", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "☁️", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "🌴️", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.DND, "", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.AWAY, "", targetContext),
                width,
                2,
                targetContext
            )

            fragment.addBitmap(
                BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.OFFLINE, "", targetContext),
                width,
                2,
                targetContext
            )
        }

        shortSleep()
        waitForIdleSync()
        screenshot(sut)
    }
}
