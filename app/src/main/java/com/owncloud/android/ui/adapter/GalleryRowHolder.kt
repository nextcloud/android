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
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClientManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.glide.CustomGlideStreamLoader
import java.net.URLEncoder

class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    val storageManager: FileDataStorageManager,
    private val galleryAdapter: GalleryAdapter,
    private val user: User,
    private val clientFactory: ClientFactory
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    private lateinit var currentRow: GalleryRow

    fun bind(row: GalleryRow) {
        currentRow = row

        val shrinkRatio = computeShrinkRatio(row)
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
        val baseUri = client.getClientFor(user.toOwnCloudAccount(), context).baseUri
        val previewLink = "/index.php/core/preview.png?file="
        val mode = "&a=1&mode=cover&forceIcon=0"

        row.files.forEach { file ->
            val thumbnail = ImageView(context)

            val width = ((file.imageDimension?.width ?: defaultThumbnailSize) * shrinkRatio).toInt()
            val height = ((file.imageDimension?.height ?: defaultThumbnailSize) * shrinkRatio).toInt()

            val imageUrl: String = (((baseUri.toString() + previewLink
                + URLEncoder.encode(file.remotePath, Charsets.UTF_8.name())
                + "&x=" + (width)) + "&y=" + (height)) + mode)

            Glide
                .with(context)
                .using(CustomGlideStreamLoader(user, clientFactory))
                .load(imageUrl)
                .asBitmap()
                .placeholder(R.drawable.file_image)
                .error(R.drawable.background)
                .override(height, width)
                .fitCenter()
                .into(thumbnail)

            thumbnail.setOnClickListener {
                ocFileListDelegate.ocFileListFragmentInterface.onItemClicked(file)
            }

            binding.rowLayout.addView(thumbnail)
        }

        /*
           // re-use existing ones
        while (binding.rowLayout.childCount < row.files.size) {
            val shimmer = LoaderImageView(context).apply {
                setImageResource(R.drawable.background)
                resetLoader()
                invalidate()
            }

            val bitmap = BitmapUtils.drawableToBitmap(
                ResourcesCompat.getDrawable(context.resources, R.drawable.file_image, null),
                defaultThumbnailSize.toInt(),
                defaultThumbnailSize.toInt()
            )

            val drawable = ThumbnailsCacheManager.AsyncGalleryImageDrawable(
                context.resources,
                bitmap,
                null
            )

            val thumbnail = ImageView(context).apply {
                setImageDrawable(drawable)
            }

            val layout = LinearLayout(context).apply {
                addView(shimmer)
                addView(thumbnail)
            }

            binding.rowLayout.addView(layout)
        }

        if (binding.rowLayout.childCount > row.files.size) {
            binding.rowLayout.removeViewsInLayout(row.files.size - 1, (binding.rowLayout.childCount - row.files.size))
        }

        val shrinkRatio = computeShrinkRatio(row)

        for (indexedFile in row.files.withIndex()) {
            adjustFile(indexedFile, shrinkRatio, row)
        }
         */

    }

    private fun adjustFile(indexedFile: IndexedValue<OCFile>, shrinkRatio: Float, row: GalleryRow) {
        val file = indexedFile.value
        val index = indexedFile.index

        val adjustedHeight1 = ((file.imageDimension?.height ?: defaultThumbnailSize) * shrinkRatio).toInt()
        val adjustedWidth1 = ((file.imageDimension?.width ?: defaultThumbnailSize) * shrinkRatio).toInt()

        // re-use existing one
        val linearLayout = binding.rowLayout[index] as LinearLayout
        val shimmer = linearLayout[0] as LoaderImageView

        val thumbnail = linearLayout[1] as ImageView

        thumbnail.adjustViewBounds = true
        thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER

        ocFileListDelegate.bindGalleryRowThumbnail(
            shimmer,
            thumbnail,
            file,
            this,
            adjustedWidth1
        )

        val params = LinearLayout.LayoutParams(adjustedWidth1, adjustedHeight1)

        val zero = context.resources.getInteger(R.integer.zero)
        val margin = context.resources.getInteger(R.integer.small_margin)
        if (index < (row.files.size - 1)) {
            params.setMargins(zero, zero, margin, margin)
        } else {
            params.setMargins(zero, zero, zero, margin)
        }

        thumbnail.layoutParams = params
        thumbnail.layoutParams.height = adjustedHeight1
        thumbnail.layoutParams.width = adjustedWidth1

        shimmer.layoutParams = params
        shimmer.layoutParams.height = adjustedHeight1
        shimmer.layoutParams.width = adjustedWidth1
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
