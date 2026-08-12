/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums.util

import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.nextcloud.utils.extensions.PARENT_BOTTOM
import com.nextcloud.utils.extensions.PARENT_END
import com.nextcloud.utils.extensions.PARENT_START
import com.nextcloud.utils.extensions.PARENT_TOP
import com.nextcloud.utils.extensions.anchor
import com.nextcloud.utils.extensions.bottomOf
import com.nextcloud.utils.extensions.endOf
import com.nextcloud.utils.extensions.fillParent
import com.nextcloud.utils.extensions.startOf
import com.owncloud.android.databinding.AlbumImageThumbnailBinding
import com.owncloud.android.databinding.AlbumSharingBottomSheetBinding

/**
 * Positions the album preview images inside the collage. The XML already describes the
 * [MAX_IMAGES] case, every smaller count is rebuilt here with a [androidx.constraintlayout.widget.ConstraintSet].
 */
class AlbumCollageLayout(private val binding: AlbumSharingBottomSheetBinding) {

    val images: List<AlbumImageThumbnailBinding> = listOf(
        binding.imgTopLeft,
        binding.imgBottomLeft,
        binding.imgCenter,
        binding.imgTopRight,
        binding.imgBottomRight
    )

    private val topLeft = binding.imgTopLeft.root.id
    private val bottomLeft = binding.imgBottomLeft.root.id
    private val center = binding.imgCenter.root.id
    private val topRight = binding.imgTopRight.root.id

    fun arrange(count: Int) {
        if (count == MAX_IMAGES) {
            return
        }

        val set = ConstraintSet().apply { clone(binding.imageCollage) }

        images.forEach {
            set.clear(it.root.id)
            it.root.visibility = View.GONE
        }

        when (count) {
            SINGLE_IMAGE -> set.singleImage()
            TWO_IMAGES -> set.twoImages()
            THREE_IMAGES -> set.threeImages()
            FOUR_IMAGES -> set.fourImages()
        }

        set.applyTo(binding.imageCollage)
    }

    private fun ConstraintSet.singleImage() = fillParent(topLeft)

    private fun ConstraintSet.twoImages() {
        listOf(topLeft, bottomLeft).forEach { constrainPercentWidth(it, TWO_COLUMN_WIDTH_PERCENT) }

        anchor(topLeft, top = PARENT_TOP, bottom = PARENT_BOTTOM, start = PARENT_START, end = startOf(bottomLeft))
        anchor(bottomLeft, top = PARENT_TOP, bottom = PARENT_BOTTOM, start = endOf(topLeft), end = PARENT_END)
    }

    private fun ConstraintSet.threeImages() {
        listOf(topLeft, bottomLeft, center).forEach { constrainPercentWidth(it, TWO_COLUMN_WIDTH_PERCENT) }
        listOf(topLeft, bottomLeft).forEach { constrainPercentHeight(it, TWO_ROW_HEIGHT_PERCENT) }
        constrainPercentHeight(center, FULL_HEIGHT_PERCENT)

        anchor(topLeft, top = PARENT_TOP, start = PARENT_START, end = startOf(center))
        anchor(bottomLeft, top = bottomOf(topLeft), bottom = PARENT_BOTTOM, start = PARENT_START, end = startOf(center))
        anchor(center, top = PARENT_TOP, bottom = PARENT_BOTTOM, start = endOf(topLeft), end = PARENT_END)
    }

    private fun ConstraintSet.fourImages() {
        listOf(topLeft, bottomLeft, center, topRight).forEach { constrainPercentWidth(it, TWO_COLUMN_WIDTH_PERCENT) }
        listOf(topLeft, topRight).forEach { constrainPercentHeight(it, STAGGERED_HEIGHT_PERCENT_BIG) }
        listOf(bottomLeft, center).forEach { constrainPercentHeight(it, STAGGERED_HEIGHT_PERCENT_SMALL) }

        anchor(topLeft, top = PARENT_TOP, start = PARENT_START, end = startOf(center))
        anchor(center, top = PARENT_TOP, start = endOf(topLeft), end = PARENT_END)
        anchor(
            bottomLeft,
            top = bottomOf(topLeft),
            bottom = PARENT_BOTTOM,
            start = PARENT_START,
            end = startOf(topRight)
        )
        anchor(topRight, top = bottomOf(center), bottom = PARENT_BOTTOM, start = endOf(bottomLeft), end = PARENT_END)
    }

    companion object {
        const val MAX_IMAGES = 5

        private const val SINGLE_IMAGE = 1
        private const val TWO_IMAGES = 2
        private const val THREE_IMAGES = 3
        private const val FOUR_IMAGES = 4

        private const val TWO_COLUMN_WIDTH_PERCENT = 0.48f
        private const val TWO_ROW_HEIGHT_PERCENT = 0.48f
        private const val STAGGERED_HEIGHT_PERCENT_BIG = 0.52f
        private const val STAGGERED_HEIGHT_PERCENT_SMALL = 0.43f
        private const val FULL_HEIGHT_PERCENT = 0.98f
    }
}
