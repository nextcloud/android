/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText

@SuppressLint("ClickableViewAccessibility")
class PassCodeEditText(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {

    init {
        disableFocusChangeViaTap()
    }

    private fun disableFocusChangeViaTap() {
        setSelectAllOnFocus(false)
        setTextIsSelectable(false)
        setOnTouchListener { _: View?, _: MotionEvent? -> true }
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        val isBackButtonPressed = (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP)
        if (isBackButtonPressed) {
            // Override default behaviour and prevent dismissing the keyboard
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
