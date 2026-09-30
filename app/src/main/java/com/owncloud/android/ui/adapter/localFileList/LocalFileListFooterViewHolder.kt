/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

internal class LocalFileListFooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val footerText: TextView = itemView.findViewById(R.id.footerText)
}
