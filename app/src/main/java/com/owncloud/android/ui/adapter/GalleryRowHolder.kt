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
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val defaultThumbnailSize: Float,
    private val ocFileListDelegate: OCFileListDelegate,
    val storageManager: FileDataStorageManager,
    galleryAdapter: GalleryAdapter,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    private lateinit var currentRow: GalleryRow

    // Cached values
    private val zero by lazy { context.resources.getInteger(R.integer.zero) }
    private val smallMargin by lazy { context.resources.getInteger(R.integer.small_margin) }
    private val iconRadius by lazy { context.resources.getDimension(R.dimen.activity_icon_radius) }
    private val standardMargin by lazy { context.resources.getDimension(R.dimen.standard_margin) }
    private val checkBoxMargin by lazy { context.resources.getDimension(R.dimen.standard_quarter_padding) }

    private val checkedDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_checkbox_marked)?.also {
            viewThemeUtils.platform.tintDrawable(context, it, ColorRole.PRIMARY)
        }
    }

    private val uncheckedDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_checkbox_blank_outline)
    }

    private var lastFileCount = -1
    // endregion

    fun bind(row: GalleryRow) {
        currentRow = row
        val requiredCount = row.files.size

        // Only rebuild if file count changed
        if (lastFileCount != requiredCount) {
            binding.rowLayout.removeAllViews()
            row.files.forEach { file ->
                binding.rowLayout.addView(getRowLayout(file))
            }
            lastFileCount = requiredCount
        }

        val dimensions = getDimensions(row)

        for (i in row.files.indices) {
            val dim = dimensions.getOrNull(i) ?: (defaultThumbnailSize.toInt() to defaultThumbnailSize.toInt())
            adjustFile(i, row.files[i], dim, row)
        }
    }

    fun updateRowVisuals() = bind(currentRow)

    private fun getRowLayout(file: OCFile): FrameLayout {
        val (width, height) = OCFileUtils.getImageSize(file, defaultThumbnailSize)

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
            layoutParams = FrameLayout.LayoutParams(width, height)
        }

        val drawable = OCFileUtils.getMediaPlaceholder(file, width to height)
        val rowCellImageView = ImageView(context).apply {
            setImageDrawable(drawable)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(width, height)
        }

        return FrameLayout(context).apply {
            addView(shimmer)
            addView(rowCellImageView)
            addView(checkbox)
        }
    }

    private fun getDimensions(row: GalleryRow): List<Pair<Int, Int>> {
        val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
        val marginPx = smallMargin.toFloat()
        val totalMargins = marginPx * (row.files.size - 1)
        val availableWidth = screenWidthPx - totalMargins

        val aspectRatios = row.files.map { file ->
            val (w, h) = OCFileUtils.getImageSize(file, defaultThumbnailSize)
            if (h > 0) w.toFloat() / h else 1.0f
        }

        val sumAspectRatios = aspectRatios.sum()

        // calculate row height based on aspect ratios
        val rowHeightFloat = if (sumAspectRatios > 0) availableWidth / sumAspectRatios else defaultThumbnailSize
        val finalHeight = rowHeightFloat.toInt()

        // for each aspect ratio calculate widths
        val finalWidths = aspectRatios.map { ratio -> (rowHeightFloat * ratio).toInt() }.toMutableList()
        val usedWidth = finalWidths.sum()

        // based on screen width get remaining pixels
        val remainingPixels = (availableWidth - usedWidth).toInt()

        // add to remaining pixels to last image
        if (remainingPixels > 0 && finalWidths.isNotEmpty()) {
            val lastIndex = finalWidths.lastIndex
            finalWidths[lastIndex] = finalWidths[lastIndex] + remainingPixels
        }

        return finalWidths.map { w -> w to finalHeight }
    }

    private fun adjustFile(index: Int, file: OCFile, dims: Pair<Int, Int>, row: GalleryRow) {
        val (width, height) = dims
        val frameLayout = binding.rowLayout[index] as FrameLayout
        val shimmer = frameLayout[0] as LoaderImageView
        val thumbnail = frameLayout[1] as ImageView
        val checkbox = frameLayout[2] as ImageView

        val isChecked = ocFileListDelegate.isCheckedFile(file)
        adjustRowCell(thumbnail, isChecked)
        adjustCheckBox(checkbox, isChecked)

        ocFileListDelegate.bindGalleryRow(shimmer, thumbnail, file, this, dims)

        val endMargin = if (index < row.files.size - 1) smallMargin else zero
        thumbnail.layoutParams = FrameLayout.LayoutParams(width, height).apply {
            setMargins(0, 0, endMargin, smallMargin)
        }
        shimmer.layoutParams = FrameLayout.LayoutParams(width, height)
        frameLayout.requestLayout()
    }

    @Suppress("MagicNumber")
    private fun adjustRowCell(imageView: ImageView, isChecked: Boolean) {
        val scale = if (isChecked) 0.8f else 1.0f
        val radius = if (isChecked) iconRadius else 0f
        imageView.scaleX = scale
        imageView.scaleY = scale
        imageView.makeRounded(context, radius)
    }

    private fun adjustCheckBox(imageView: ImageView, isChecked: Boolean) {
        if (ocFileListDelegate.isMultiSelect) {
            val checkboxDrawable = if (isChecked) checkedDrawable else uncheckedDrawable

            checkboxDrawable?.apply {
                val margin = standardMargin.toInt()
                setBounds(margin, margin, margin, margin)
            }

            // Only set if different
            if (imageView.drawable !== checkboxDrawable) {
                imageView.setImageDrawable(checkboxDrawable)
            }
        }

        imageView.setVisibleIf(ocFileListDelegate.isMultiSelect)
    }
}
