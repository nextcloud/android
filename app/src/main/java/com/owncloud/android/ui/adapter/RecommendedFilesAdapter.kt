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
import com.owncloud.android.databinding.GridItemBinding
import com.owncloud.android.datamodel.OCFile

class RecommendedFilesAdapter(
    private val fileListAdapter: OCFileListAdapter,
    private val recommendations: ArrayList<OCFile>
) : RecyclerView.Adapter<OCFileListGridItemViewHolder>() {

    companion object {
        private const val LAYOUT_ITEM_WIDTH = 120f
    }

    fun getItemPosition(file: OCFile): Int {
        return recommendations.indexOf(file)
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
        fileListAdapter.bindRecommendedFilesHolder(holder, item)
    }

    // region Private Methods
    private fun ConstraintLayout.setLayoutItemWidth() {
        layoutParams = layoutParams.apply {
            width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                LAYOUT_ITEM_WIDTH,
                resources.displayMetrics
            ).toInt()
        }
    }
    // endregion
}
