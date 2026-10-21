/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.filelist.recommended

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.RecommendedFileItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.filelist.OCFileListAdapter

class RecommendedFilesAdapter(
    private val fileListAdapter: OCFileListAdapter,
    private val recommendations: List<OCFile>
) : RecyclerView.Adapter<RecommendedFilesViewHolder>() {

    fun getItemPosition(file: OCFile): Int = recommendations.indexOf(file)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedFilesViewHolder {
        val binding = RecommendedFileItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return RecommendedFilesViewHolder(binding)
    }

    override fun getItemCount(): Int = recommendations.size

    override fun onBindViewHolder(holder: RecommendedFilesViewHolder, position: Int) {
        val item = recommendations[position]
        fileListAdapter.bindRecommendedFilesHolder(holder, item)
        holder.reason.text = item.reason
    }
}
