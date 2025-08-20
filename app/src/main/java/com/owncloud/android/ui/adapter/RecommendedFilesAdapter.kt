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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OCFileListGridItemViewHolder {
        val binding = GridItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return OCFileListGridItemViewHolder(binding).apply {
            setLayoutItemWidth(binding.ListItemLayout)
        }
    }

    override fun getItemCount(): Int = recommendations.size

    @Suppress("MagicNumber")
    override fun onBindViewHolder(holder: OCFileListGridItemViewHolder, position: Int) {
        val item = recommendations.elementAt(position)
        holder.binding.run {
            val filePath = item.directory + OCFile.PATH_SEPARATOR + item.name
            fileListAdapter.bindRecommendedFileHolder(holder, filePath)
        }
    }

    private fun setLayoutItemWidth(layout: ConstraintLayout) {
        val layoutItemWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            LAYOUT_ITEM_WIDTH,
            layout.resources.displayMetrics
        ).toInt()

        layout.run {
            val params = layoutParams.apply {
                width = layoutItemWidth
            }
            layoutParams = params
        }
    }
}
