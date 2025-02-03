/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.utils.theme

import android.util.TypedValue
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.nextcloud.android.common.ui.util.buildColorStateList
import com.owncloud.android.R

class IonosMaterialViewThemeUtils {

    fun colorMaterialButtonPrimaryTonal(button: MaterialButton) {}

    fun colorMaterialButtonPrimaryBorderless(button: MaterialButton) {}

    fun colorMaterialButtonPrimaryFilled(button: MaterialButton) {}

    fun colorMaterialButtonText(button: MaterialButton) {}

    fun themeSnackbar(snackbar: Snackbar) {}

    fun colorMaterialButtonPrimaryOutlined(button: MaterialButton) {}

    fun colorTextInputLayout(textInputLayout: TextInputLayout) {
        val context = textInputLayout.context

        val boxStrokeWidthRes = R.dimen.text_input_box_stroke_width
        val cornerRadiusRes = R.dimen.text_input_box_corner_radius
        val boxStrokeColorStateList = buildColorStateList(
            -android.R.attr.state_focused to context.getColor(R.color.text_input_border_stroke_color),
            android.R.attr.state_focused to context.getColor(R.color.text_input_focused_border_stroke_color),
        )

        textInputLayout.setBoxStrokeWidthResource(boxStrokeWidthRes)
        textInputLayout.setBoxStrokeWidthFocusedResource(boxStrokeWidthRes)
        textInputLayout.setBoxCornerRadiiResources(cornerRadiusRes, cornerRadiusRes, cornerRadiusRes, cornerRadiusRes)
        textInputLayout.setBoxStrokeColorStateList(boxStrokeColorStateList)

        val errorColorStateList = buildColorStateList(
            -android.R.attr.state_focused to context.getColor(R.color.text_input_error_color),
            android.R.attr.state_focused to context.getColor(R.color.text_input_error_color),
        )

        textInputLayout.setErrorIconTintList(errorColorStateList)
        textInputLayout.setErrorTextColor(errorColorStateList)
        textInputLayout.boxStrokeErrorColor = errorColorStateList

        val hintTextColorStateList = buildColorStateList(
            -android.R.attr.state_focused to context.getColor(R.color.text_input_hint_text_color),
            android.R.attr.state_focused to context.getColor(R.color.text_input_focused_hint_text_color),
        )

        textInputLayout.defaultHintTextColor = hintTextColorStateList

        val padding = context.resources.getDimension(R.dimen.text_input_padding).toInt()
        val textSize = context.resources.getDimension(R.dimen.text_input_text_size)

        textInputLayout.editText?.setPadding(padding)
        textInputLayout.editText?.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        textInputLayout.editText?.highlightColor = context.getColor(R.color.text_input_highlight_color)
    }
}
