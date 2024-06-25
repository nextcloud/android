/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.UnifiedSearchEmptyBinding
import com.owncloud.android.databinding.UnifiedSearchFooterBinding
import com.owncloud.android.databinding.UnifiedSearchHeaderBinding
import com.owncloud.android.databinding.UnifiedSearchItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.InitDiskCacheTask
import com.owncloud.android.ui.interfaces.UnifiedSearchListInterface
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchSection
import com.owncloud.android.utils.theme.ViewThemeUtils

/**
 * This Adapter populates a SectionedRecyclerView with search results by unified search
 */
@Suppress("LongParameterList")
class UnifiedSearchListAdapter(
    private val storageManager: FileDataStorageManager,
    private val listInterface: UnifiedSearchListInterface,
    private val filesAction: UnifiedSearchItemViewHolder.FilesAction,
    private val user: User,
    private val clientFactory: ClientFactory,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    companion object {
        private const val VIEW_TYPE_EMPTY = Int.MAX_VALUE
    }

    private var sections: List<UnifiedSearchSection> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = UnifiedSearchHeaderBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchHeaderViewHolder(binding, viewThemeUtils, context)
            }
            VIEW_TYPE_FOOTER -> {
                val binding = UnifiedSearchFooterBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchFooterViewHolder(binding, context, listInterface)
            }
            VIEW_TYPE_ITEM -> {
                val binding = UnifiedSearchItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
                UnifiedSearchItemViewHolder(
                    binding,
                    user,
                    clientFactory,
                    storageManager,
                    listInterface,
                    filesAction,
                    context,
                    viewThemeUtils
                )
            }
            VIEW_TYPE_EMPTY -> {
                val binding = UnifiedSearchEmptyBinding.inflate(layoutInflater, parent, false)
                EmptyViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    internal class EmptyViewHolder(binding: UnifiedSearchEmptyBinding) :
        SectionedViewHolder(binding.getRoot())

    override fun getSectionCount(): Int {
        return sections.size
    }

    override fun getItemCount(section: Int): Int {
        return sections[section].entries.size
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        (holder as UnifiedSearchHeaderViewHolder).run {
            bind(sections[section])
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {
        if (sections[section].hasMoreResults) {
            (holder as UnifiedSearchFooterViewHolder).run {
                bind(sections[section])
            }
        }
    }

    override fun getFooterViewType(section: Int): Int = when {
        sections[section].hasMoreResults -> VIEW_TYPE_FOOTER
        else -> VIEW_TYPE_EMPTY
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        // TODO different binding (and also maybe diff UI) for non-file results
        (holder as UnifiedSearchItemViewHolder).run {
            val entry = sections[section].entries[relativePosition]
            bind(entry)
        }
    }

    override fun onViewAttachedToWindow(holder: SectionedViewHolder) {
        if (holder is UnifiedSearchItemViewHolder) {
            holder.binding.thumbnailShimmer.run {
                if (visibility == View.VISIBLE) {
                    setImageResource(R.drawable.background)
                    resetLoader()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(sections: List<UnifiedSearchSection>) {
        this.sections = sections
        notifyDataSetChanged()
    }

    init {
        // initialise thumbnails cache on background thread
        InitDiskCacheTask().execute()
    }
}
