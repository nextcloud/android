/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.get
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.glide.CustomGlideStreamLoader
import java.net.URLEncoder

class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    private val galleryAdapter: GalleryAdapter,
    private val user: User,
    private val clientFactory: ClientFactory
) : SectionedViewHolder(binding.root) {

    private val context = galleryAdapter.context
    private lateinit var currentRow: GalleryRow

    private val client = OwnCloudClientManagerFactory.getDefaultSingleton()

    @Suppress("DEPRECATION")
    private val baseUri = client.getClientFor(user.toOwnCloudAccount(), context).baseUri
    private val previewLink = "/index.php/core/preview.png?file="
    private val mode = "&a=1&mode=cover&forceIcon=0"

    fun bind(row: GalleryRow) {
        currentRow = row

        addImages(row)

        if (binding.rowLayout.childCount > row.files.size) {
            binding.rowLayout.removeViewsInLayout(row.files.size - 1, (binding.rowLayout.childCount - row.files.size))
        }

        adjustImages(row)
    }

    private fun addImages(row: GalleryRow) {
        row.files.forEach { file ->
            val thumbnail = ImageView(context)

            val imageUrl: String = (((baseUri.toString() + previewLink
                + URLEncoder.encode(file.remotePath, Charsets.UTF_8.name())
                + "&x=" + (defaultThumbnailSize)) + "&y=" + (defaultThumbnailSize)) + mode)

            Glide
                .with(context)
                .using(CustomGlideStreamLoader(user, clientFactory))
                .load(imageUrl)
                .asBitmap()
                .placeholder(R.drawable.file_image)
                .error(R.drawable.background)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(thumbnail)

            val layout = LinearLayout(context)
            layout.addView(thumbnail)

            thumbnail.setOnClickListener {
                ocFileListDelegate.ocFileListFragmentInterface.onItemClicked(file)
            }

            binding.rowLayout.addView(layout)
        }
    }

    private fun adjustImages(row: GalleryRow) {
        row.files.forEachIndexed { index, file ->
            val shrinkRatio = computeShrinkRatio(row)

            val height = ((file.imageDimension?.height ?: defaultThumbnailSize) * shrinkRatio).toInt()
            val width = ((file.imageDimension?.width ?: defaultThumbnailSize) * shrinkRatio).toInt()

            val linearLayout = binding.rowLayout[index] as LinearLayout
            val thumbnail = linearLayout[0] as ImageView

            thumbnail.adjustViewBounds = true
            thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER

            val params = LinearLayout.LayoutParams(width, height)

            val zero = context.resources.getInteger(R.integer.zero)
            val margin = context.resources.getInteger(R.integer.small_margin)
            if (index < (row.files.size - 1)) {
                params.setMargins(zero, zero, margin, margin)
            } else {
                params.setMargins(zero, zero, zero, margin)
            }

            thumbnail.layoutParams = params
            thumbnail.layoutParams.width = width
            thumbnail.layoutParams.height = height
        }
    }

    fun redraw() {
        bind(currentRow)
    }

    @SuppressWarnings("MagicNumber", "ComplexMethod")
    private fun computeShrinkRatio(row: GalleryRow): Float {
        val screenWidth =
            DisplayUtils.convertDpToPixel(context.resources.configuration.screenWidthDp.toFloat(), context)
                .toFloat()

        if (row.files.size > 1) {
            var newSummedWidth = 0f
            for (file in row.files) {
                // first adjust all thumbnails to max height
                val thumbnail1 = file.imageDimension ?: ImageDimension(defaultThumbnailSize, defaultThumbnailSize)

                val height1 = thumbnail1.height
                val width1 = thumbnail1.width

                val scaleFactor1 = row.getMaxHeight() / height1
                val newHeight1 = height1 * scaleFactor1
                val newWidth1 = width1 * scaleFactor1

                file.imageDimension = ImageDimension(newWidth1, newHeight1)

                newSummedWidth += newWidth1
            }

            var c = 1f
            // this ensures that files in last row are better visible,
            // e.g. when 2 images are there, it uses 2/5 of screen
            if (galleryAdapter.columns == 5) {
                when (row.files.size) {
                    2 -> {
                        c = 5 / 2f
                    }
                    3 -> {
                        c = 4 / 3f
                    }
                    4 -> {
                        c = 4 / 5f
                    }
                    5 -> {
                        c = 1f
                    }
                }
            }

            return (screenWidth / c) / newSummedWidth
        } else {
            val thumbnail1 = row.files[0].imageDimension ?: ImageDimension(defaultThumbnailSize, defaultThumbnailSize)
            return (screenWidth / galleryAdapter.columns) / thumbnail1.width
        }
    }
}
