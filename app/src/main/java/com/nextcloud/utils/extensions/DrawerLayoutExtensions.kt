/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout

fun DrawerLayout.padDrawerContentForSystemBars(header: View, footer: View) {
    val headerTopPadding = header.paddingTop
    val footerBottomPadding = footer.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        header.updatePadding(top = headerTopPadding + systemBars.top)
        footer.updatePadding(bottom = footerBottomPadding + systemBars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
