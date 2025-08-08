/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.graphics.BitmapFactory
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.TextDrawable
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AvatarIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.AvatarIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showAvatars() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val avatarRadius = targetContext.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
                    val width = DisplayUtils.convertDpToPixel(2 * avatarRadius, targetContext)
                    val fragment = AvatarTestFragment()

                    sut.addFragment(fragment)
                    fragment.run {
                        addAvatar("Admin", avatarRadius, width, targetContext)
                        addAvatar("Test Server Admin", avatarRadius, width, targetContext)
                        addAvatar("Cormier Paulette", avatarRadius, width, targetContext)
                        addAvatar("winston brent", avatarRadius, width, targetContext)
                        addAvatar("Baker James Lorena", avatarRadius, width, targetContext)
                        addAvatar("Baker  James   Lorena", avatarRadius, width, targetContext)
                        addAvatar("email@nextcloud.localhost", avatarRadius, width, targetContext)
                    }

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showAvatars", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showAvatarsWithStatus() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val avatarRadius = targetContext.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
                    val width = DisplayUtils.convertDpToPixel(2 * avatarRadius, targetContext)
                    val fragment = AvatarTestFragment()

                    val paulette = BitmapFactory.decodeFile(getFile("paulette.jpg").absolutePath)
                    val christine = BitmapFactory.decodeFile(getFile("christine.jpg").absolutePath)
                    val textBitmap = BitmapUtils.drawableToBitmap(TextDrawable.createNamedAvatar("Admin", avatarRadius))

                    sut.addFragment(fragment)

                    fragment.run {
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(paulette, StatusType.ONLINE, "😘", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "☁️", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "🌴️", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(christine, StatusType.ONLINE, "", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(paulette, StatusType.DND, "", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(christine, StatusType.AWAY, "", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(paulette, StatusType.OFFLINE, "", targetContext),
                            width * 2,
                            1,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "😘", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "☁️", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "🌴️", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.ONLINE, "", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.DND, "", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.AWAY, "", targetContext),
                            width,
                            2,
                            targetContext
                        )
                        addBitmap(
                            BitmapUtils.createAvatarWithStatus(textBitmap, StatusType.OFFLINE, "", targetContext),
                            width,
                            2,
                            targetContext
                        )
                    }
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showAvatarsWithStatus", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
