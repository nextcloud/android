/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.OCFileUtils
import com.nextcloud.utils.extensions.makeRounded
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    val storageManager: FileDataStorageManager,
    private val galleryAdapter: GalleryAdapter,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    private lateinit var currentRow: GalleryRow
    private val zero by lazy { context.resources.getInteger(R.integer.zero) }
    private val smallMargin by lazy { context.resources.getInteger(R.integer.small_margin) }
    private val iconRadius by lazy { context.resources.getDimension(R.dimen.activity_icon_radius) }
    private val standardMargin by lazy { context.resources.getDimension(R.dimen.standard_margin) }
    private val checkBoxMargin by lazy { context.resources.getDimension(R.dimen.standard_quarter_padding) }

    fun bind(row: GalleryRow) {
        currentRow = row

        // re-use existing ones
        while (binding.rowLayout.childCount < row.files.size) {
            val rowLayout = getRowLayout()
            binding.rowLayout.addView(rowLayout)
        }

        if (binding.rowLayout.childCount > row.files.size) {
            binding.rowLayout.removeViews(row.files.size, binding.rowLayout.childCount - row.files.size)
        }

        val shrinkRatio = computeShrinkRatio(row)

        for (indexedFile in row.files.withIndex()) {
            adjustFile(indexedFile, shrinkRatio, row)
        }
    }

    private fun getRowLayout(): FrameLayout {
        val checkbox = ImageView(context).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = checkBoxMargin.toInt()
                topMargin = checkBoxMargin.toInt()
            }
        }

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
        val rowCellImageView = ImageView(context).apply {
            setImageDrawable(drawable)
        }

        return FrameLayout(context).apply {
            addView(shimmer)
            addView(rowCellImageView)
            addView(checkbox)
        }
    }

    fun redraw() {
        bind(currentRow)
    }

    @SuppressWarnings("MagicNumber")
    private fun computeShrinkRatio(row: GalleryRow): Float {
        val screenWidth = DisplayUtils.convertDpToPixel(
            context.resources.configuration.screenWidthDp.toFloat(),
            context
        ).toFloat()

        return if (row.files.size > 1) {
            computeMultiFileShrinkRatio(row, screenWidth)
        } else {
            computeSingleFileShrinkRatio(row, screenWidth)
        }
    }

    private fun computeMultiFileShrinkRatio(row: GalleryRow, screenWidth: Float): Float {
        val targetHeight = row.getMaxHeight()
        var totalUnscaledWidth = 0f

        for (file in row.files) {
            val (originalWidth, originalHeight) = OCFileUtils.getImageSize(file, defaultThumbnailSize)

            val scaledWidth = targetHeight * (originalWidth.toFloat() / originalHeight)
            file.imageDimension = ImageDimension(scaledWidth, targetHeight)

            totalUnscaledWidth += scaledWidth
        }

        val totalAvailableWidth = screenWidth - ((row.files.size - 1) * smallMargin)
        return totalAvailableWidth / totalUnscaledWidth
    }

    private fun computeSingleFileShrinkRatio(row: GalleryRow, screenWidth: Float): Float {
        val width = OCFileUtils.getImageSize(row.files[0], defaultThumbnailSize).first
        return (screenWidth / galleryAdapter.columns) / width
    }

    private fun adjustFile(indexedFile: IndexedValue<OCFile>, shrinkRatio: Float, row: GalleryRow) {
        val file = indexedFile.value
        val index = indexedFile.index

        val width = file.imageDimension?.width?.times(shrinkRatio)?.toInt() ?: 0
        val height = file.imageDimension?.height?.times(shrinkRatio)?.toInt() ?: 0

        val frameLayout = binding.rowLayout[index] as FrameLayout
        val checkBoxImageView = frameLayout[2] as ImageView
        val shimmer = frameLayout[0] as LoaderImageView
        val thumbnail = (frameLayout[1] as ImageView).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_XY
        }

        val isChecked = ocFileListDelegate.isCheckedFile(file)

        adjustRowCell(thumbnail, isChecked)
        adjustCheckBox(checkBoxImageView, isChecked)

        ocFileListDelegate.bindGalleryRowThumbnail(
            shimmer,
            thumbnail,
            file,
            this,
            width
        )

        // Force layout update
        frameLayout.requestLayout()

        val params = FrameLayout.LayoutParams(width, height)
        val endMargin = if (index < row.files.size - 1) smallMargin else zero
        params.setMargins(zero, zero, endMargin, smallMargin)

        thumbnail.layoutParams = params
        shimmer.layoutParams = FrameLayout.LayoutParams(params)
    }

    @Suppress("MagicNumber")
    private fun adjustRowCell(imageView: ImageView, isChecked: Boolean) {
        imageView.apply {
            scaleX = if (isChecked) 0.8f else 1.0f
            scaleY = scaleX
            makeRounded(context, if (isChecked) iconRadius else 0f)
        }
    }

    private fun adjustCheckBox(imageView: ImageView, isChecked: Boolean) {
        if (ocFileListDelegate.isMultiSelect) {
            val checkboxDrawable = (
                if (isChecked) {
                    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_checkbox_marked)
                    drawable?.let {
                        viewThemeUtils.platform.tintDrawable(context, drawable, ColorRole.PRIMARY)
                    }
                    drawable
                } else {
                    ContextCompat.getDrawable(context, R.drawable.ic_checkbox_blank_outline)
                }
                )?.apply {
                val margin = standardMargin.toInt()
                setBounds(margin, margin, margin, margin)
            }
            imageView.setImageDrawable(checkboxDrawable)
        }

        imageView.setVisibleIf(ocFileListDelegate.isMultiSelect)
    }
}
