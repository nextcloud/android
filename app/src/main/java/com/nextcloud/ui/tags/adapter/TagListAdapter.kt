/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.ui.tags.adapter.viewholder.CreateTagViewHolder
import com.nextcloud.ui.tags.adapter.viewholder.TagViewHolder
import com.owncloud.android.R
import com.owncloud.android.lib.resources.tags.Tag

class TagListAdapter(private val onTagChecked: (Tag, Boolean) -> Unit, private val onCreateTag: (String) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    override fun getItemViewType(position: Int): Int =
        if (showCreateItem && position == tags.size) VIEW_TYPE_CREATE else VIEW_TYPE_TAG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_CREATE) {
            val view = inflater.inflate(R.layout.tag_list_item, parent, false)
            CreateTagViewHolder(view, onCreateTag)
        } else {
            val view = inflater.inflate(R.layout.tag_list_item, parent, false)
            TagViewHolder(view, onTagChecked)
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
}
