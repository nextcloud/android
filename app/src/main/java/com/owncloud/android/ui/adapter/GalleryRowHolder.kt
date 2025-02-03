/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.get
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.ionos.annotation.IonosCustomization
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.databinding.IonosItemMediaBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.BitmapUtils

@IonosCustomization("Simplified grid")
class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    val storageManager: FileDataStorageManager,
    private val galleryAdapter: GalleryAdapter
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    lateinit var currentRow: GalleryRow

    fun bind(row: GalleryRow) {
        currentRow = row

        // re-use existing ones
        while (binding.rowLayout.childCount < galleryAdapter.columns) {
            val itemBinding = IonosItemMediaBinding.inflate(
                LayoutInflater.from(context),
                binding.rowLayout,
                true,
            )

            itemBinding.shimmer.apply {
                setImageResource(R.drawable.background)
                resetLoader()
                invalidate()
            }

            itemBinding.image.apply {
                setImageDrawable(
                    ThumbnailsCacheManager.AsyncGalleryImageDrawable(
                        context.resources,
                        BitmapUtils.drawableToBitmap(
                            ResourcesCompat.getDrawable(resources, R.drawable.file_image, null),
                            defaultThumbnailSize.toInt(),
                            defaultThumbnailSize.toInt()
                        ),
                        null
                    )
                )
            }
        }

        if (binding.rowLayout.childCount > galleryAdapter.columns) {
            binding.rowLayout.removeViewsInLayout(row.files.size - 1, (binding.rowLayout.childCount - row.files.size))
        }

        binding.rowLayout.children.take(row.files.size)
            .forEach { it.visibility = VISIBLE }
        binding.rowLayout.children.drop(row.files.size)
            .forEach { it.visibility = INVISIBLE }

        for (indexedFile in row.files.withIndex()) {
            adjustFile(indexedFile, row)
        }
    }

    fun redraw() {
        bind(currentRow)
    }

    private fun adjustFile(indexedFile: IndexedValue<OCFile>, row: GalleryRow) {
        val file = indexedFile.value
        val index = indexedFile.index

        val linearLayout = binding.rowLayout[index] as ConstraintLayout
        val shimmer = linearLayout[0] as LoaderImageView
        val thumbnail = linearLayout[1] as ImageView

        ocFileListDelegate.bindGalleryRowThumbnail(
            shimmer,
            thumbnail,
            file,
            this,
            thumbnail.width
        )

        val zero = context.resources.getInteger(R.integer.zero)
        val margin = context.resources.getInteger(R.integer.small_margin)
        if (index < (row.files.size - 1)) {
            (thumbnail.layoutParams as ConstraintLayout.LayoutParams).setMargins(zero, zero, margin, margin)
            (shimmer.layoutParams as ConstraintLayout.LayoutParams).setMargins(zero, zero, margin, margin)
        } else {
            (thumbnail.layoutParams as ConstraintLayout.LayoutParams).setMargins(zero, zero, zero, margin)
            (shimmer.layoutParams as ConstraintLayout.LayoutParams).setMargins(zero, zero, zero, margin)
        }
    }

}
