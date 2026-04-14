/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags.adapter.viewholder

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

class CreateTagViewHolder(
    itemView: View,
    private val onCreateTag: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val colorDot: View = itemView.findViewById(R.id.tag_color_dot)
    private val tagName: TextView = itemView.findViewById(R.id.tag_name)
    private val checkBox: CheckBox = itemView.findViewById(R.id.tag_checkbox)

    fun bind(name: String) {
        colorDot.visibility = View.INVISIBLE
        tagName.text = itemView.context.getString(R.string.create_tag_format, name)
        checkBox.visibility = View.GONE

        itemView.setOnClickListener {
            onCreateTag(name)
        }
    }
}
