/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter

import android.content.Context
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.owncloud.android.lib.common.SearchResult
import java.util.ArrayList
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import kotlin.NotImplementedError
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.ThumbnailsCacheManager.InitDiskCacheTask

/**
 * This Adapter populates a SectionedRecyclerView with search results by unified search
 */
class UnifiedSearchListAdapter(
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val user: User,
    private val clientFactory: ClientFactory,
    private val context: Context
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    companion object {
        private const val FILES_PROVIDER_ID = "files"
    }

    private var list: List<SearchResult> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = UnifiedSearchHeaderBinding.inflate(
                LayoutInflater.from(
                    context
                ),
                parent,
                false
            )
            UnifiedSearchHeaderViewHolder(binding, context)
        } else {
            val binding = UnifiedSearchItemBinding.inflate(
                LayoutInflater.from(
                    context
                ),
                parent,
                false
            )
            UnifiedSearchItemViewHolder(
                binding,
                user,
                clientFactory,
                storageManager,
                listInterface,
                context
            )
        }
    }

    override fun getSectionCount(): Int {
        return list.size
    }

    override fun getItemCount(section: Int): Int {
        return list[section].entries.size
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        val headerViewHolder = holder as UnifiedSearchHeaderViewHolder
        headerViewHolder.bind(list[section])
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {
        throw NotImplementedError()
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        // TODO different binding (and also maybe diff UI) for non-file results
        val itemViewHolder = holder as UnifiedSearchItemViewHolder
        val entry = list[section].entries[relativePosition]
        itemViewHolder.bind(entry)
    }

    override fun onViewAttachedToWindow(holder: SectionedViewHolder) {
        if (holder is UnifiedSearchItemViewHolder) {
            val thumbnailShimmer = holder.binding.thumbnailShimmer
            if (thumbnailShimmer.visibility == View.VISIBLE) {
                thumbnailShimmer.setImageResource(R.drawable.background)
                thumbnailShimmer.resetLoader()
            }
        }
    }

    fun setData(results: Map<String, SearchResult>) {
        // "Files" always goes first
        val comparator =
            Comparator { o1: Map.Entry<String, SearchResult>, o2: Map.Entry<String, SearchResult> ->
                when {
                    o1.key == FILES_PROVIDER_ID -> -1
                    o2.key == FILES_PROVIDER_ID -> 1
                    else -> 0
                }
            }

        list = results.asSequence().sortedWith(comparator).map { it.value }.toList()
        // TODO only update where needed
        notifyDataSetChanged()
    }

    init {
        // initialise thumbnails cache on background thread
        InitDiskCacheTask().execute()
    }
}
