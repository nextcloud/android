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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.extensions.createRoundedOutline
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.databinding.GalleryUnsupportedCellBinding
import com.owncloud.android.datamodel.GalleryCellSize
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.theme.ViewThemeUtils

private const val CHECKED_SCALE = 0.8f
private const val UNCHECKED_SCALE = 1.0f
private const val SELECTION_ANIMATION_DURATION_MS = 150L

private const val SELECTION_BACKGROUND_INDEX = 0
private const val SHIMMER_INDEX = 1
private const val THUMBNAIL_INDEX = 2
private const val UNSUPPORTED_INDEX = 3
private const val CHECKBOX_INDEX = 4

class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val ocFileListDelegate: OCFileListDelegate,
    galleryAdapter: GalleryAdapter,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    private val zero by lazy { context.resources.getInteger(R.integer.zero) }
    private val smallMargin by lazy { context.resources.getInteger(R.integer.small_margin) }
    private val checkBoxMargin by lazy { context.resources.getDimensionPixelSize(R.dimen.standard_half_margin) }

    private val selectedOutline by lazy {
        val resources = context.resources
        val radiusDp = resources.getDimension(R.dimen.activity_icon_radius) / resources.displayMetrics.density
        createRoundedOutline(context, radiusDp)
    }

    private val checkedDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_checkbox_marked)?.also {
            viewThemeUtils.platform.tintDrawable(context, it, ColorRole.PRIMARY)
        }
    }

    private val checkedBackground by lazy {
        ContextCompat.getDrawable(context, R.drawable.gallery_selection_checked_background)
    }

    private val uncheckedDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.gallery_selection_unchecked)
    }

    fun bind(row: GalleryRow) {
        ensureCellCount(row.files.size)

        row.files.forEachIndexed { index, file ->
            val size = row.cellSizes.getOrNull(index) ?: return@forEachIndexed
            bindCell(index, file, size, isLast = index == row.files.lastIndex)
        }
    }

    fun recycle() {
        for (index in 0 until binding.rowLayout.childCount) {
            ocFileListDelegate.cancelGalleryRow(thumbnailAt(index))
        }
    }

    private fun ensureCellCount(count: Int) {
        if (binding.rowLayout.childCount == count) {
            return
        }

        binding.rowLayout.removeAllViews()
        repeat(count) { binding.rowLayout.addView(createCell()) }
    }

    private fun createCell(): FrameLayout {
        val selectionBackground = View(context).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(0, 0)
            viewThemeUtils.platform.colorViewBackground(this, ColorRole.SURFACE_CONTAINER_HIGHEST)
        }

        val shimmer = LoaderImageView(context).apply {
            setImageResource(R.drawable.background)
            resetLoader()
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        val thumbnail = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        val checkbox = ImageView(context).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = checkBoxMargin
                topMargin = checkBoxMargin
            }
        }

        return FrameLayout(context).apply {
            addView(selectionBackground)
            addView(shimmer)
            addView(thumbnail)
            addView(GalleryUnsupportedCellBinding.inflate(LayoutInflater.from(context), this, false).root)
            addView(checkbox)
        }
    }

    private fun bindCell(index: Int, file: OCFile, size: GalleryCellSize, isLast: Boolean) {
        val frameLayout = binding.rowLayout[index] as FrameLayout
        val selectionBackground = frameLayout[SELECTION_BACKGROUND_INDEX]
        val shimmer = frameLayout[SHIMMER_INDEX] as LoaderImageView
        val thumbnail = frameLayout[THUMBNAIL_INDEX] as ImageView
        val unsupported = GalleryUnsupportedCellBinding.bind(frameLayout[UNSUPPORTED_INDEX])
        val checkbox = frameLayout[CHECKBOX_INDEX] as ImageView

        val endMargin = if (isLast) zero else smallMargin
        applyCellSize(shimmer, size, endMargin = zero, bottomMargin = zero)
        applyCellSize(thumbnail, size, endMargin = endMargin, bottomMargin = smallMargin)
        applyCellSize(unsupported.root, size, endMargin = endMargin, bottomMargin = smallMargin)
        applyCellSize(selectionBackground, size, endMargin = endMargin, bottomMargin = smallMargin)

        val isChecked = ocFileListDelegate.isCheckedFile(file)
        val isSameFileRebound = thumbnail.tag == file.fileId
        selectionBackground.setVisibleIf(isChecked)
        applySelection(thumbnail, isChecked, isSameFileRebound)
        applySelection(unsupported.root, isChecked, isSameFileRebound)
        applyCheckBox(checkbox, isChecked)

        ocFileListDelegate.bindGalleryRow(shimmer, thumbnail, unsupported, file, this)
    }

    private fun applyCellSize(view: View, size: GalleryCellSize, endMargin: Int, bottomMargin: Int) {
        val params = view.layoutParams as FrameLayout.LayoutParams

        val unchanged = params.width == size.width &&
            params.height == size.height &&
            params.rightMargin == endMargin &&
            params.bottomMargin == bottomMargin

        if (unchanged) {
            return
        }

        params.width = size.width
        params.height = size.height
        params.setMargins(0, 0, endMargin, bottomMargin)
        view.layoutParams = params
    }

    private fun applySelection(view: View, isChecked: Boolean, isSameFileRebound: Boolean) {
        view.outlineProvider = if (isChecked) selectedOutline else ViewOutlineProvider.BACKGROUND
        view.clipToOutline = isChecked

        val scale = if (isChecked) CHECKED_SCALE else UNCHECKED_SCALE
        view.animate().cancel()

        if (!isSameFileRebound) {
            view.scaleX = scale
            view.scaleY = scale
            return
        }

        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(SELECTION_ANIMATION_DURATION_MS)
            .start()
    }

    private fun applyCheckBox(imageView: ImageView, isChecked: Boolean) {
        val isMultiSelect = ocFileListDelegate.isMultiSelect
        imageView.setVisibleIf(isMultiSelect)
        if (!isMultiSelect) {
            return
        }

        val checkboxDrawable = if (isChecked) checkedDrawable else uncheckedDrawable
        if (imageView.drawable !== checkboxDrawable) {
            imageView.setImageDrawable(checkboxDrawable)
            imageView.background = if (isChecked) checkedBackground else null
        }
    }

    private fun thumbnailAt(index: Int): ImageView =
        (binding.rowLayout[index] as FrameLayout)[THUMBNAIL_INDEX] as ImageView
}
