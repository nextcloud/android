/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.utils.extensions.createRoundedOutline
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.GalleryCellBinding
import com.owncloud.android.databinding.GalleryRowBinding
import com.owncloud.android.datamodel.GalleryCellSize
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.theme.ViewThemeUtils

private const val PLACEHOLDER_ICON_INSET_RATIO = 0.32f

class GalleryRowHolder(
    val binding: GalleryRowBinding,
    private val ocFileListDelegate: OCFileListDelegate,
    galleryAdapter: GalleryAdapter,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedViewHolder(binding.root) {
    val context = galleryAdapter.context

    private val cells = mutableListOf<GalleryCellBinding>()

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

    private val uncheckedDrawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.gallery_selection_unchecked)
    }

    fun bind(row: GalleryRow) {
        ensureCellCount(row.files.size)

        row.files.forEachIndexed { index, file ->
            val size = row.cellSizes.getOrNull(index) ?: return@forEachIndexed
            bindCell(cells[index], file, size)
        }
    }

    fun recycle() {
        cells.forEach { ocFileListDelegate.cancelGalleryRow(it.thumbnail) }
    }

    private fun ensureCellCount(count: Int) {
        if (cells.size == count) {
            return
        }

        binding.rowLayout.removeAllViews()
        cells.clear()
        repeat(count) {
            val cell = createCell()
            cells.add(cell)
            binding.rowLayout.addView(cell.root)
        }
    }

    private fun createCell(): GalleryCellBinding =
        GalleryCellBinding.inflate(LayoutInflater.from(context), binding.rowLayout, false).apply {
            viewThemeUtils.platform.colorViewBackground(selectionBackground, ColorRole.SURFACE_CONTAINER_HIGHEST)
        }

    private fun bindCell(cell: GalleryCellBinding, file: OCFile, size: GalleryCellSize) = with(cell) {
        root.applySize(size)

        val isChecked = ocFileListDelegate.isCheckedFile(file)
        val isSameFileRebound = thumbnail.tag == file.fileId
        applySelection(cell, isChecked, isSameFileRebound)
        applyCheckBox(checkbox, isChecked)

        ocFileListDelegate.bindGalleryRow(
            shimmer,
            thumbnail,
            unsupported,
            file,
            this@GalleryRowHolder,
            placeholderInset(size)
        )
    }

    private fun placeholderInset(size: GalleryCellSize): Int =
        (minOf(size.width, size.height) * PLACEHOLDER_ICON_INSET_RATIO).toInt()

    private fun View.applySize(size: GalleryCellSize) {
        if (layoutParams.width == size.width && layoutParams.height == size.height) {
            return
        }

        updateLayoutParams {
            width = size.width
            height = size.height
        }
    }

    private fun applySelection(cell: GalleryCellBinding, isChecked: Boolean, isSameFileRebound: Boolean) {
        cell.selectionBackground.setVisibleIf(isChecked)
        cell.thumbnail.applySelectedOutline(isChecked)
        cell.unsupported.root.applySelectedOutline(isChecked)

        cell.root.isSelected = isChecked
        if (!isSameFileRebound) {
            cell.root.jumpDrawablesToCurrentState()
        }
    }

    private fun View.applySelectedOutline(isChecked: Boolean) {
        outlineProvider = if (isChecked) selectedOutline else ViewOutlineProvider.BACKGROUND
        clipToOutline = isChecked
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
        }
    }
}
