/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.view.View
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.utils.DisplayUtils

class OCFileListGridController {

    companion object {
        private const val TAG = "OCFileListGridController"

        // Reserved space for file extension text in DP
        private const val EXTENSION_RESERVED_DP: Int = 36

        // Padding between text and more icon in DP
        private const val SAFETY_MARGIN_DP: Int = 8

        // extra spacing between text and more button
        private const val MORE_BUTTON_MARGIN_DP: Int = 8
    }

    private var lastScreenWidth = -1
    private var lastColumnCount = -1
    private var cachedFolderMaxWidth = -1
    private var cachedFileMaxWidth = -1

    private fun configureFilenameMaxWidth(
        holder: OCFileListGridItemViewHolder,
        file: ServerFileInterface,
        columnCount: Int
    ) {
        val context = MainApp.getAppContext()
        if (context == null) {
            Log_OC.w(TAG, "Grid layout max file width configuration cancelled, context is null")
            return
        }

        val screenWidth = DisplayUtils.convertDpToPixel(
            context.resources.configuration.screenWidthDp.toFloat(),
            context
        )

        // dont recalculate same value
        if (columnCount != lastColumnCount || screenWidth != lastScreenWidth) {
            // available width per column
            val cellWidth = screenWidth / columnCount

            val moreButtonPx = context.resources.getDimensionPixelSize(R.dimen.iconized_single_line_item_icon_size)

            // 3-4 chars width

            val density = context.resources.displayMetrics.density
            val extensionMinPx = (density * EXTENSION_RESERVED_DP).toInt()
            val paddingPx = (density * SAFETY_MARGIN_DP).toInt()
            val moreButtonMarginPx = (density * MORE_BUTTON_MARGIN_DP).toInt()

            // name + more button
            cachedFolderMaxWidth = cellWidth - moreButtonPx - paddingPx - moreButtonMarginPx

            // name + extension + more button
            cachedFileMaxWidth = cellWidth - moreButtonPx - extensionMinPx - paddingPx - moreButtonMarginPx

            // fallback
            if (cachedFolderMaxWidth < 0) {
                cachedFolderMaxWidth = context.resources.getDimensionPixelSize(
                    R.dimen.grid_container_default_max_file_name
                )
            }
            if (cachedFileMaxWidth < 0) {
                cachedFileMaxWidth = context.resources.getDimensionPixelSize(
                    R.dimen.grid_container_default_max_file_name
                )
            }

            lastColumnCount = columnCount
            lastScreenWidth = screenWidth
        }

        holder.bidiFilename.maxWidth = if (file.isFolder) cachedFolderMaxWidth else cachedFileMaxWidth
    }

    fun invalidateGridLayoutCachedWidths() {
        lastColumnCount = -1
        lastScreenWidth = -1
    }

    private fun useBidiSpecificLayout(
        gridItemViewHolder: OCFileListGridItemViewHolder,
        filenamePair: Pair<String?, String?>,
        file: OCFile,
        columnCount: Int
    ) {
        val (base, ext) = filenamePair

        gridItemViewHolder.run {
            more.visibility = View.GONE
            fileName.visibility = View.GONE
            binding.bidiFilenameContainer.visibility = View.VISIBLE

            configureFilenameMaxWidth(gridItemViewHolder, file, columnCount)
            bidiFilename.text = base
            extension?.text = ext
        }
    }

    private fun useNormalFilenameLayout(
        gridItemViewHolder: OCFileListGridItemViewHolder,
        filenamePair: Pair<String?, String?>,
        isFolder: Boolean
    ) {
        val (base, ext) = filenamePair

        gridItemViewHolder.run {
            more.visibility = View.VISIBLE
            fileName.visibility = View.VISIBLE
            binding.bidiFilenameContainer.visibility = View.GONE

            val displayName = if (isFolder) base else base + ext
            fileName.text = displayName
            extension?.visibility = View.GONE
        }
    }

    fun handleGridMode(
        gridItemViewHolder: OCFileListGridItemViewHolder,
        filenamePair: Pair<String?, String?>,
        file: OCFile,
        containsBidiControlCharacters: Boolean,
        isFolder: Boolean,
        columnCount: Int
    ) {
        if (containsBidiControlCharacters) {
            useBidiSpecificLayout(gridItemViewHolder, filenamePair, file, columnCount)
        } else {
            useNormalFilenameLayout(gridItemViewHolder, filenamePair, isFolder)
        }
    }
}
