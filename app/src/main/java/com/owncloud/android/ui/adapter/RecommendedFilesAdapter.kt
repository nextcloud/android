/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.RecommendedFilesListItemBinding
import com.owncloud.android.utils.DisplayUtils

// TODO delete mock data
data class Recommendation(
    val id: Long,
    val timestamp: Long,
    val name: String,
    val directory: String,
    val extension: String,
    val mimeType: String,
    val hasPreview: Boolean,
    val reason: String
)

class RecommendedFilesAdapter(
    private val context: Context,
    private val recommendations: List<Recommendation>,
    private val delegate: OCFileListDelegate
) : RecyclerView.Adapter<RecommendedFilesAdapter.RecommendedFilesViewHolder>() {

    inner class RecommendedFilesViewHolder(val binding: RecommendedFilesListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // TODO onclick item
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedFilesViewHolder {
        val binding = RecommendedFilesListItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return RecommendedFilesViewHolder(binding)
    }

    override fun getItemCount(): Int = recommendations.size

    override fun onBindViewHolder(holder: RecommendedFilesViewHolder, position: Int) {
        val item = recommendations[position]

        holder.binding.run {
            name.text = item.name
            timestamp.text = DisplayUtils.getRelativeTimestamp(context,  item.timestamp)
            delegate.setThumbnailFromFileId(thumbnail, shimmerThumbnail, item.id)
        }
    }
}
