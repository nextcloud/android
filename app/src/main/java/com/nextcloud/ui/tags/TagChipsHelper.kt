/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.owncloud.android.R
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.utils.theme.ViewThemeUtils

class TagChipsHelper(
    private val viewThemeUtils: ViewThemeUtils
) {
    fun refresh(
        context: Context,
        chipGroup: ChipGroup,
        tags: List<Tag>,
        onEditClicked: Runnable
    ) {
        chipGroup.removeAllViews()
        chipGroup.visibility = android.view.View.VISIBLE

        for (tag in tags) {
            val chip = Chip(context).apply {
                text = tag.name
                chipBackgroundColor = ColorStateList.valueOf(
                    context.resources.getColor(R.color.bg_default, context.theme)
                )
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(100.0f)
                    .build()
                isClickable = false
            }
            chip.setEnsureMinTouchTargetSize(false)
            viewThemeUtils.material.themeChipSuggestion(chip)

            if (tag.color != null) {
                val color = Color.parseColor(tag.color)
                chip.chipStrokeColor = ColorStateList.valueOf(color)
                chip.setTextColor(color)
            }

            chipGroup.addView(chip)
        }

        val editChip = Chip(context).apply {
            setChipIconResource(R.drawable.ic_edit)
            setText(R.string.manage_tags)
            chipBackgroundColor = ColorStateList.valueOf(
                context.resources.getColor(R.color.bg_default, context.theme)
            )
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(100.0f)
                .build()
            setOnClickListener { onEditClicked.run() }
        }
        editChip.setEnsureMinTouchTargetSize(false)
        viewThemeUtils.material.themeChipSuggestion(editChip)
        chipGroup.addView(editChip)
    }
}
