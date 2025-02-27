/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class DurationPickerDialogFragmentIT : AbstractIT() {

    private val testClassName = "com.owncloud.android.ui.dialog.DurationPickerDialogFragmentIT"

    @Test
    @UiThread
    @ScreenshotTest
    fun showDurationDialog() {
        val initialDuration = DAYS.toMillis(2) + HOURS.toMillis(8) + MINUTES.toMillis(15)

        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val transaction = sut.supportFragmentManager.beginTransaction()

                val dialog = DurationPickerDialogFragment.newInstance(
                    initialDuration,
                    "Dialog title",
                    "Hint message"
                )
                dialog.show(transaction, "DURATION_DIALOG")

                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "showDurationDialog", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
