/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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
