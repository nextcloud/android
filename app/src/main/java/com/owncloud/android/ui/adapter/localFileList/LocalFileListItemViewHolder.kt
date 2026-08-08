/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.view.View
import android.widget.TextView
import com.owncloud.android.R

internal class LocalFileListItemViewHolder(itemView: View) : LocalFileListGridItemViewHolder(itemView) {
    val fileSize: TextView = itemView.findViewById(R.id.file_size)
    val fileSeparator: TextView = itemView.findViewById(R.id.file_separator)
    val lastModification: TextView = itemView.findViewById(R.id.last_mod)

    init {
        itemView.findViewById<View>(R.id.sharedAvatars).visibility = View.GONE
        itemView.findViewById<View>(R.id.overflow_menu).visibility = View.GONE
        itemView.findViewById<View>(R.id.tagsGroup).visibility = View.GONE
    }
}
