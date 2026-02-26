/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.test.TestActivity
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class SnackbarTests {

    class NormalTestFragment : Fragment()
    class DialogTestFragment : DialogFragment()
    class BottomSheetTestFragment : BottomSheetDialogFragment()

    @Before
    fun setUp() {
        Intents.init()
        val cancelledResult = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
        intending(
            anyOf(
                hasComponent(AuthenticatorActivity::class.java.name),
                hasComponent(FirstRunActivity::class.java.name)
            )
        ).respondWith(cancelledResult)
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun assertSnackbarVisible(msg: String) {
        onView(withText(msg)).check(matches(isDisplayed()))
    }

    private fun assertSnackbarVisible(@StringRes msgRes: Int) {
        onView(withText(msgRes)).check(matches(isDisplayed()))
    }

    private fun testFragmentSnackbar(fragment: Fragment, @StringRes msgRes: Int) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.addFragment(fragment)
            }
            scenario.onActivity {
                DisplayUtils.showSnackMessage(fragment, msgRes)
            }
            assertSnackbarVisible(msgRes)
        }
    }

    @Test
    fun testNormalFragmentSnackbar() {
        testFragmentSnackbar(NormalTestFragment(), R.string.app_name)
    }

    @Test
    fun testDialogFragmentSnackbar() {
        testFragmentSnackbar(DialogTestFragment(), R.string.app_name)
    }

    @Test
    fun testBottomSheetFragmentSnackbar() {
        testFragmentSnackbar(BottomSheetTestFragment(), R.string.app_name)
    }

    @Test
    fun testNullFragmentSnackbarShouldNotCrash() {
        DisplayUtils.showSnackMessage(null as Fragment?, R.string.app_name)
    }

    @Test
    fun testActivityStringResSnackbar() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                DisplayUtils.showSnackMessage(sut, R.string.app_name)
            }
            assertSnackbarVisible(R.string.app_name)
        }
    }

    @Test
    fun testActivityStringSnackbar() {
        launchActivity<TestActivity>().use { scenario ->
            var message = ""
            scenario.onActivity { sut ->
                message = sut.getString(R.string.app_name)
                DisplayUtils.showSnackMessage(sut, message)
            }
            assertSnackbarVisible(message)
        }
    }

    @Test
    fun testViewStringResSnackbar() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                val contentView = sut.findViewById<android.view.View>(android.R.id.content)
                DisplayUtils.showSnackMessage(contentView, R.string.app_name)
            }
            assertSnackbarVisible(R.string.app_name)
        }
    }

    @Test
    fun testViewStringSnackbar() {
        launchActivity<TestActivity>().use { scenario ->
            var message = ""
            scenario.onActivity { sut ->
                message = sut.getString(R.string.app_name)
                val contentView = sut.findViewById<android.view.View>(android.R.id.content)
                DisplayUtils.showSnackMessage(contentView, message)
            }
            assertSnackbarVisible(message)
        }
    }
}
