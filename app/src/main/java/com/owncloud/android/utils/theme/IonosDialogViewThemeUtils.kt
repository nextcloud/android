/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.utils.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.ionos.annotation.IonosCustomization
import com.nextcloud.android.common.ui.theme.utils.DialogViewThemeUtils
import com.owncloud.android.R

class IonosDialogViewThemeUtils(
    private val delegate: DialogViewThemeUtils,
) {

    @IonosCustomization
    fun colorDialogMenuText(button: MaterialButton) {
        button.setTextColor(button.context.getColor(R.color.text_color))
        button.iconTint = ColorStateList.valueOf(button.context.getColor(R.color.default_icon_color))
    }

    @IonosCustomization
    fun colorMaterialAlertDialogBackground(context: Context, dialogBuilder: MaterialAlertDialogBuilder) {
        val materialShapeDrawable = MaterialShapeDrawable(
            context,
            null,
            com.google.android.material.R.attr.alertDialogStyle,
            com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents,
        )
        materialShapeDrawable.initializeElevationOverlay(context)
        materialShapeDrawable.fillColor = ColorStateList.valueOf(context.getColor(R.color.bg_default))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val radius = context.resources.getDimension(R.dimen.dialogBorderRadius)
            materialShapeDrawable.setCornerSize(radius)
        }

        dialogBuilder.background = materialShapeDrawable
    }

    fun colorDialogHeadline(textView: TextView) {
        delegate.colorDialogHeadline(textView)
    }

    fun colorDialogIcon(icon: ImageView) {
        delegate.colorDialogIcon(icon)
    }

    fun colorDialogSupportingText(textView: TextView) {
        delegate.colorDialogSupportingText(textView)
    }

    fun colorMaterialAlertDialogIcon(context: Context, drawableId: Int): Drawable {
        return delegate.colorMaterialAlertDialogIcon(context, drawableId)
    }
}
