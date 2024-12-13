/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R
import com.owncloud.android.databinding.RecommendedFilesListItemBinding
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

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
    private val viewThemeUtils: ViewThemeUtils,
    private val recommendations: List<Recommendation>
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

        holder.binding.name.text = item.name
        // holder.binding.timestamp.text = DisplayUtils.getRelativeTimestamp(context,  item.timestamp)

        val thumbnail = getThumbnail(item)

        /*
        val centerPixel = thumbnail.getPixel(thumbnail.width / 2, thumbnail.height / 2)

        val redValue = Color.red(centerPixel)
        val blueValue = Color.blue(centerPixel)
        val greenValue = Color.green(centerPixel)

        val centerColor = Color.argb(0.8f, redValue.toFloat(), greenValue.toFloat(), blueValue.toFloat())
         */

        val containerColor = ContextCompat.getColor(context, R.color.primary)
        holder.binding.container.backgroundTintList = ColorStateList.valueOf(containerColor)
        holder.binding.icon.setImageBitmap(thumbnail)
    }

    private fun getThumbnail(item: Recommendation): Bitmap {
        var drawable = MimeTypeUtil.getFileTypeIcon(
            item.mimeType,
            item.name,
            context,
            viewThemeUtils
        )

        if (drawable == null) {
            drawable = ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.file_image,
                null
            )
        }

        if (drawable == null) {
            drawable = ColorDrawable(Color.GRAY)
        }

        val width = DisplayUtils.convertPixelToDp(40, context).toInt()
        return BitmapUtils.drawableToBitmap(drawable, width / 2, width / 2)
    }
}
