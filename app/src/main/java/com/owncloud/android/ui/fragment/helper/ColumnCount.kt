/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.helper

import android.content.res.Resources
import com.nextcloud.utils.extensions.isLandscape

enum class ColumnCount(val landscape: Int, val portrait: Int) {
    Wide(5, 2),
    Normal(4, 2);

    fun get(isLandscape: Boolean): Int =
        if (isLandscape) landscape else portrait

    companion object {
        fun get(isWide: Boolean, isLandscape: Boolean): Int =
            (if (isWide) Wide else Normal).get(isLandscape)
    }
}
