/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import javax.inject.Inject

class KeyboardUtils @Inject constructor() {

    fun showKeyboardForEditText(editText: EditText) {
        editText.requestFocus()
        // needs 100ms delay to account for focus animations
        editText.postDelayed({
            val context = editText.context
            if (context != null) {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
        }, SHOW_INPUT_DELAY_MILLIS)
    }

    companion object {
        private const val SHOW_INPUT_DELAY_MILLIS = 100L
    }
}
