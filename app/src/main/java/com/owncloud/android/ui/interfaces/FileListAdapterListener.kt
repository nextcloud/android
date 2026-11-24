/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.interfaces

import android.view.View
import com.owncloud.android.datamodel.OCFile

interface FileListAdapterListener {
    fun onShareIconClick(file: OCFile)

    fun showShareDetailView(file: OCFile)

    fun showActivityDetailView(file: OCFile)

    fun onOverflowIconClicked(file: OCFile)

    fun onItemClicked(file: OCFile)

    fun onItemClicked(file: OCFile, sourceView: View) {
        onItemClicked(file)
    }

    fun onLongItemClicked(file: OCFile): Boolean

    val isLoading: Boolean

    fun onHeaderClicked()
}
