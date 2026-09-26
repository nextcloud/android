/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

internal open class LocalFileListGridItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val fileName: TextView = itemView.findViewById(R.id.Filename)
    val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    val checkbox: ImageView = itemView.findViewById(R.id.custom_checkbox)
    val itemLayout: LinearLayout = itemView.findViewById(R.id.ListItemLayout)

    init {
        itemView.findViewById<View>(R.id.sharedIcon).visibility = View.GONE
        itemView.findViewById<View>(R.id.favorite_action).visibility = View.GONE
        itemView.findViewById<View>(R.id.localFileIndicator).visibility = View.GONE
    }
}
