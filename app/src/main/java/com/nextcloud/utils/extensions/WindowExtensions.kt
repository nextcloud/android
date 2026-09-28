/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.appcompat.app.ActionBar

fun ActionBar.toggle(show: Boolean) {
    if (show) {
        show()
    } else {
        hide()
    }
}
