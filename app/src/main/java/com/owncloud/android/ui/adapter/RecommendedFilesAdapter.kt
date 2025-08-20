/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.recommendations.Recommendation
import com.owncloud.android.databinding.RecommendedFilesListItemBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

class RecommendedFilesAdapter(
    private val recommendations: ArrayList<Recommendation>,
    private val delegate: OCFileListDelegate,
    private val onItemClickListener: OnItemClickListener,
    private val storageManager: FileDataStorageManager
) : RecyclerView.Adapter<RecommendedFilesAdapter.RecommendedFilesViewHolder>() {

    interface OnItemClickListener {
        fun selectRecommendedFile(file: OCFile)
        fun showRecommendedFileMoreActions(file: OCFile, view: View)
    }

    inner class RecommendedFilesViewHolder(val binding: RecommendedFilesListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendedFilesViewHolder {
        val binding = RecommendedFilesListItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return RecommendedFilesViewHolder(binding)
    }

    override fun getItemCount(): Int = recommendations.size

    @Suppress("MagicNumber")
    override fun onBindViewHolder(holder: RecommendedFilesViewHolder, position: Int) {
        val item = recommendations.elementAt(position)

        holder.binding.run {
            name.text = item.name
            reason.text = item.reason

            val filePath = item.directory + OCFile.PATH_SEPARATOR + item.name
            val file = storageManager.getFileByDecryptedRemotePath(filePath) ?: return

            delegate.setThumbnail(thumbnail, shimmerThumbnail, file)

            container.setOnClickListener {
                onItemClickListener.selectRecommendedFile(file)
            }

            moreAction.setOnClickListener {
                onItemClickListener.showRecommendedFileMoreActions(file, holder.itemView)
            }
        }
    }
}
