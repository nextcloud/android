/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.graphics.BitmapFactory
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.nextcloud.test.TestActivity
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
