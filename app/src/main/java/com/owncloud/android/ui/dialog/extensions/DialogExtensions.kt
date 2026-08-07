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

fun Dialog?.themeButtons(util: ViewThemeUtils?) {
    val alertDialog = this as? AlertDialog ?: return
    val material = util?.material ?: return

    with(alertDialog) {
        (getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton)?.let {
            material.colorMaterialButtonPrimaryTonal(it)
        }
        (getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton)?.let {
            material.colorMaterialButtonPrimaryBorderless(it)
        }
        (getButton(AlertDialog.BUTTON_NEUTRAL) as? MaterialButton)?.let {
            material.colorMaterialButtonPrimaryBorderless(it)
        }
    }
}
