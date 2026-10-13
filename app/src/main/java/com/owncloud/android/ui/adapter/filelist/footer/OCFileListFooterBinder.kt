/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist.footer

import android.app.Activity
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.theme.ViewThemeUtils

internal class OCFileListFooterBinder(
    private val activity: Activity,
    private val viewThemeUtils: ViewThemeUtils,
    private val preferences: AppPreferences,
    private val fragmentInterface: OCFileListFragmentInterface
) {

    // region Public methods
    fun bind(holder: OCFileListFooterViewHolder, files: List<OCFile>) {
        holder.footerText.text = fileCountSummary(files)
        viewThemeUtils.platform.colorCircularProgressBar(holder.loadingProgressBar, ColorRole.ON_SURFACE_VARIANT)
        holder.loadingProgressBar.setVisibleIf(fragmentInterface.isLoading)
    }
    // endregion

    // region Private methods
    private fun fileCountSummary(files: List<OCFile>): String {
        val showHiddenFiles = preferences.isShowHiddenFilesEnabled
        val foldersCount = files.count { it.isFolder }
        val filesCount = files.count { !it.isFolder && (!it.isHidden || showHiddenFiles) }

        return listOfNotNull(
            quantityText(R.plurals.file_list__footer__file, filesCount),
            quantityText(R.plurals.file_list__footer__folder, foldersCount)
        ).joinToString(SUMMARY_SEPARATOR)
    }

    private fun quantityText(pluralsRes: Int, count: Int): String? = count.takeIf { it > 0 }?.let {
        activity.resources.getQuantityString(pluralsRes, it, it)
    }
    // endregion

    companion object {
        private const val SUMMARY_SEPARATOR = ", "
    }
}
