/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.test.TestActivity
import com.owncloud.android.R
import org.junit.Test

class DisplayUtilsFragmentSnackbarTest {

    class NormalTestFragment : Fragment()
    class DialogTestFragment : DialogFragment()
    class BottomSheetTestFragment : BottomSheetDialogFragment()

    private fun testFragmentSnackbar(fragment: Fragment, @StringRes msg: Int) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                sut.addFragment(fragment)
                DisplayUtils.showSnackMessage(fragment, msg)
            }

            onView(isRoot()).check(matches(isDisplayed()))
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
}
