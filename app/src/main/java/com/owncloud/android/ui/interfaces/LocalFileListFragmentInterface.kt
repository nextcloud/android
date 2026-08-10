/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.interfaces

import java.io.File

interface LocalFileListFragmentInterface {
    val columnsCount: Int
    fun onItemClicked(file: File?)
    fun onItemCheckboxClicked(file: File?)
    fun setLoading(enabled: Boolean)
}
