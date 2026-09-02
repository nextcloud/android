/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class DirectLoginScreenshotIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.authentication.DirectLoginScreenshotIT"

    @Test
    @ScreenshotTest
    fun directLoginForm() {
        ActivityScenario.launch(AuthenticatorActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.accountSetupBinding.apply {
                    hostUrlInput.setText("https://cloud.example.com")
                    requireNotNull(browserLoginButton).visibility = View.GONE
                    requireNotNull(directLoginToggle).visibility = View.GONE
                    requireNotNull(directLoginSection).visibility = View.VISIBLE
                }
            }

            onView(isRoot()).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                screenshotViaName(activity, createName("${testClassName}_directLoginForm", ""))
            }
        }
    }
}
