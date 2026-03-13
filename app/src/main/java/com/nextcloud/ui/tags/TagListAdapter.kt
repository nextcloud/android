/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.lib.resources.tags.Tag

class TagListAdapter(
    private val onTagChecked: (Tag, Boolean) -> Unit,
    private val onCreateTag: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var tags: List<Tag> = emptyList()
    private var assignedTagIds: Set<String> = emptySet()
    private var query: String = ""
    private var showCreateItem: Boolean = false

    companion object {
        private const val VIEW_TYPE_TAG = 0
        private const val VIEW_TYPE_CREATE = 1
    }

    fun update(allTags: List<Tag>, assignedIds: Set<String>, searchQuery: String) {
        this.assignedTagIds = assignedIds
        this.query = searchQuery

        tags = if (searchQuery.isBlank()) {
            allTags
        } else {
            allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        showCreateItem = searchQuery.isNotBlank() && tags.none { it.name.equals(searchQuery, ignoreCase = true) }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = tags.size + if (showCreateItem) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if (showCreateItem && position == tags.size) VIEW_TYPE_CREATE else VIEW_TYPE_TAG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CREATE) {
            val view = inflater.inflate(R.layout.tag_list_item, parent, false)
            CreateTagViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.tag_list_item, parent, false)
            TagViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TagViewHolder -> {
                val tag = tags[position]
                holder.bind(tag, tag.id in assignedTagIds)
            }
            is CreateTagViewHolder -> {
                holder.bind(query)
            }
        }
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

    inner class CreateTagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
}
