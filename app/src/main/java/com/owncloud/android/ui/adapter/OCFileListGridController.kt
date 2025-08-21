/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.view.View
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.FileStorageUtils

class OCFileListGridController {
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

                bidiFilename.text = base
                extension?.text = ext
                binding.more.visibility = View.GONE
                binding.bidiMore.setOnClickListener {
                    fragmentInterface.onOverflowIconClicked(file, it)
                }
            } else {
                fileName.text = filename
                extension?.visibility = View.GONE
            }
        }
    }
}
