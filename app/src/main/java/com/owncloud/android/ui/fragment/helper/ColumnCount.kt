/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.helper

private const val WIDE_LANDSCAPE_COLUMNS = 5
private const val NORMAL_LANDSCAPE_COLUMNS = 4
private const val PORTRAIT_COLUMNS = 2

enum class ColumnCount(val landscape: Int, val portrait: Int) {
    Wide(WIDE_LANDSCAPE_COLUMNS, PORTRAIT_COLUMNS),
    Normal(NORMAL_LANDSCAPE_COLUMNS, PORTRAIT_COLUMNS);

    fun get(isLandscape: Boolean): Int = if (isLandscape) landscape else portrait
}
