/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.extensions

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.EspressoIdlingResource
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

inline fun <reified T : Activity> AbstractIT.launchAndCapture(
    testClassName: String,
    actionName: String,
    intent: Intent? = null,
    crossinline before: (T) -> Unit
) {
    launchActivity<T>(intent).use { scenario ->
        scenario.onActivity { activity ->
            onIdleSync {
                EspressoIdlingResource.increment()
                before(activity)
                EspressoIdlingResource.decrement()

                val screenshotName = createName(testClassName + "_" + actionName, "")
                onView(isRoot()).check(matches(isDisplayed()))
                screenshotViaName(activity, screenshotName)
            }
        }
    }
}
