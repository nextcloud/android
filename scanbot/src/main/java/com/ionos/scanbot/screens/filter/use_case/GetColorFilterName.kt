/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter.use_case

import android.content.Context
import com.ionos.scanbot.R
import com.ionos.scanbot.filter.color.ColorFilterType

internal class GetColorFilterName(private val context: Context) {

	operator fun invoke(filterType: ColorFilterType): String = when (filterType) {
		is ColorFilterType.MagicColor -> context.getString(R.string.scanbot_filter_magic_color)
		is ColorFilterType.MagicText -> context.getString(R.string.scanbot_filter_magic_text)
		is ColorFilterType.Color -> context.getString(R.string.scanbot_filter_color)
		is ColorFilterType.Grayscale -> context.getString(R.string.scanbot_filter_grayscale)
		is ColorFilterType.BlackWhite -> context.getString(R.string.scanbot_filter_black_n_white)
		is ColorFilterType.None -> context.getString(R.string.scanbot_filter_none)
	}
}
