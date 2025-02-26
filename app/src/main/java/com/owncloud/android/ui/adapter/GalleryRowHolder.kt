/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryRowHolder(
    private val ioScope: CoroutineScope,
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    val storageManager: FileDataStorageManager,
    private val galleryAdapter: GalleryAdapter
) : SectionedViewHolder(binding.root) {
    private val tag = "GalleryRowHolder"
    val context = galleryAdapter.context

    private lateinit var currentRow: GalleryRow

    fun bind(row: GalleryRow) {
        Log_OC.d(tag, "binding gallery row")

        currentRow = row

        // re-use existing ones
        while (binding.rowLayout.childCount < row.files.size) {
            val linearLayout = getLinearLayout()
            binding.rowLayout.addView(linearLayout)
        }

        if (binding.rowLayout.childCount > row.files.size) {
            binding.rowLayout.removeViewsInLayout(row.files.size - 1, (binding.rowLayout.childCount - row.files.size))
        }

        ioScope.launch {
            val shrinkRatio = withContext(Dispatchers.Default) {
                row.computeShrinkRatio(context, defaultThumbnailSize, galleryAdapter.columns)
            }

            withContext(Dispatchers.Main) {
                for (indexedFile in row.files.withIndex()) {
                    adjustFile(indexedFile, shrinkRatio, row)
                }
            }
        }
    }

    private fun getLinearLayout(): LinearLayout {
        val shimmer = LoaderImageView(context).apply {
            setImageResource(R.drawable.background)
            resetLoader()
            invalidate()
        }

        val fileDrawable = ResourcesCompat.getDrawable(context.resources, R.drawable.file_image, null)
        val thumbnailSize = defaultThumbnailSize.toInt()
        val bitmap = BitmapUtils.drawableToBitmap(fileDrawable, thumbnailSize, thumbnailSize)
        val drawable = ThumbnailsCacheManager.AsyncGalleryImageDrawable(
            context.resources,
            bitmap,
            null
        )
        val imageView = ImageView(context).apply {
            setImageDrawable(drawable)
        }

        return LinearLayout(context).apply {
            addView(shimmer)
            addView(imageView)
        }
    }

    fun redraw() {
        bind(currentRow)
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
}
