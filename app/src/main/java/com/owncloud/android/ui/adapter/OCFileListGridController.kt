/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils

class OCFileListGridController {

    companion object {
        private const val TAG = "OCFileListGridController"

        // Reserved space for file extension text in DP
        private const val EXTENSION_RESERVED_DP: Int = 32

        // Padding between text and more icon in DP
        private const val SAFETY_MARGIN_DP: Int = 16
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

            val density = context.resources.displayMetrics.density
            val extensionMinPx = (density * EXTENSION_RESERVED_DP).toInt()
            val paddingPx = (density * SAFETY_MARGIN_DP).toInt()

            // name + more button
            cachedFolderMaxWidth = cellWidth - moreButtonPx - paddingPx

            // name + extension + more button
            cachedFileMaxWidth = cellWidth - moreButtonPx - extensionMinPx - paddingPx

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

    fun handleGridMode(
        filename: String,
        fragmentInterface: OCFileListFragmentInterface,
        gridItemViewHolder: OCFileListGridItemViewHolder,
        filenamePair: Pair<String?, String?>,
        file: OCFile
    ) {
        val containsBidiControlCharacters = FileStorageUtils.containsBidiControlCharacters(filename)
        gridItemViewHolder.run {
            fileName.setVisibleIf(!containsBidiControlCharacters)
            binding.bidiFilenameContainer.setVisibleIf(containsBidiControlCharacters)

            if (containsBidiControlCharacters) {
                val (base, ext) = filenamePair
                configureFilenameMaxWidth(this, file, fragmentInterface.getColumnsCount())
                bidiFilename.text = base
                extension?.text = ext

                val constraintLayout = gridItemViewHolder.binding.ListItemLayout
                ConstraintSet().run {
                    clone(constraintLayout)
                    clear(R.id.more, ConstraintSet.START)
                    connect(
                        R.id.more,
                        ConstraintSet.START,
                        R.id.bidi_extension,
                        ConstraintSet.END
                    )

                    applyTo(constraintLayout)
                }
            } else {
                fileName.text = filename
                extension?.visibility = View.GONE
            }
        }
    }
}
