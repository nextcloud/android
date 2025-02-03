/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.ui.activity

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable

class SingleStateDrawerArrowDrawable(
    context: Context,
    private val drawable: Drawable,
) : DrawerArrowDrawable(context) {

    override fun draw(canvas: Canvas) {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
    }
}
