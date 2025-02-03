/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery.use_case

import androidx.annotation.DrawableRes
import com.ionos.scanbot.R
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.screens.gallery.GalleryScreen.ColorFilterIcon
import javax.inject.Inject

internal class GetColorFilterIcon @Inject constructor() {

	operator fun invoke(colorFilterType: ColorFilterType): ColorFilterIcon {
		return ColorFilterIcon(
			imageRes = getImageRes(colorFilterType),
			backgroundRes = getBackgroundRes(colorFilterType),
		)
	}

	@DrawableRes
	private fun getImageRes(colorFilterType: ColorFilterType): Int {
		return when (colorFilterType) {
			is ColorFilterType.BlackWhite -> R.drawable.scanbot_ic_black_and_white
			is ColorFilterType.Color -> R.drawable.scanbot_ic_color
			is ColorFilterType.Grayscale -> R.drawable.scanbot_ic_grayscale
			is ColorFilterType.MagicColor -> R.drawable.scanbot_ic_magic_color
			is ColorFilterType.MagicText -> R.drawable.scanbot_ic_magic_text
			is ColorFilterType.None -> R.drawable.scanbot_ic_filter
		}
	}

	@DrawableRes
	private fun getBackgroundRes(colorFilterType: ColorFilterType): Int {
		return when (colorFilterType) {
			else -> 0
		}
	}
}
