/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags.adapter.viewholder

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.lib.resources.tags.Tag

class TagViewHolder(
    itemView: View,
    private val onTagChecked: (Tag, Boolean) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val colorDot: View = itemView.findViewById(R.id.tag_color_dot)
    private val tagName: TextView = itemView.findViewById(R.id.tag_name)
    private val checkBox: CheckBox = itemView.findViewById(R.id.tag_checkbox)

    fun bind(tag: Tag, isAssigned: Boolean) {
        tagName.text = tag.name

        if (tag.color != null) {
            try {
                val color = Color.parseColor(tag.color)
                val background = colorDot.background
                if (background is GradientDrawable) {
                    background.setColor(color)
                }
                colorDot.visibility = View.VISIBLE
            } catch (e: IllegalArgumentException) {
                colorDot.visibility = View.INVISIBLE
            }
        } else {
            colorDot.visibility = View.INVISIBLE
        }

        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = isAssigned
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            onTagChecked(tag, isChecked)
        }

        itemView.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }
    }
}
