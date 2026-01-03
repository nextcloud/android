/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.test

import android.view.View
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

fun withSelectedText(expected: String): Matcher<View> = object : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        description.appendText("with selected text \"$expected\"")
    }

    override fun matchesSafely(view: View): Boolean {
        if (view !is TextView) return false
        val text = view.text?.toString() ?: ""
        val s = view.selectionStart
        val e = view.selectionEnd
        if (s < 0 || e < 0 || s > e || e > text.length) return false
        return text.substring(s, e) == expected
    }
}
