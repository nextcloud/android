/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.android.lib.resources.recommendations.Recommendation
import com.owncloud.android.databinding.GridItemBinding
import com.owncloud.android.datamodel.OCFile

class RecommendedFilesAdapter(
    private val fileListAdapter: OCFileListAdapter,
    private val recommendations: ArrayList<Recommendation>,
) : RecyclerView.Adapter<OCFileListGridItemViewHolder>() {

    companion object {
        private const val LAYOUT_ITEM_WIDTH = 120f
    }

    fun notifyItemChanged(file: OCFile) {
        recommendations.indexOfFirst { it.decryptedRemotePath == file.decryptedRemotePath }
            .takeIf { it >= 0 }?.let { notifyItemChanged(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OCFileListGridItemViewHolder {
        val binding = GridItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return OCFileListGridItemViewHolder(binding).apply {
            binding.ListItemLayout.setLayoutItemWidth()
        }
    }

    override fun getItemCount(): Int = recommendations.size

    @Suppress("MagicNumber")
    override fun onBindViewHolder(holder: OCFileListGridItemViewHolder, position: Int) {
        val item = recommendations[position]
        fileListAdapter.bindRecommendedFileHolder(holder, item.decryptedRemotePath)
    }

    // region Private Methods
    private val Recommendation.decryptedRemotePath: String
        get() = directory + OCFile.PATH_SEPARATOR + name

    private fun ConstraintLayout.setLayoutItemWidth() {
        layoutParams = layoutParams.apply {
            width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, LAYOUT_ITEM_WIDTH, resources.displayMetrics
            ).toInt()
        }
    }
    // endregion
}
