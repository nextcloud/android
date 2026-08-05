/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.extensions

import android.app.Dialog
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.owncloud.android.utils.theme.ViewThemeUtils

fun Dialog.themeButtons(util: ViewThemeUtils?) {
    val dialog = (this as? AlertDialog) ?: return
    val util = util ?: return
    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
    positiveButton?.let {
        util.material.colorMaterialButtonPrimaryTonal(it)
    }

    val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
    negativeButton?.let {
        util.material.colorMaterialButtonPrimaryBorderless(it)
    }

    val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL) as? MaterialButton
    neutralButton?.let {
        util.material.colorMaterialButtonPrimaryBorderless(neutralButton)
    }
}
