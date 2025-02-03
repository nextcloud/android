/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter.view

import com.ionos.scanbot.filter.color.ColorFilterType

internal interface FilterControlsListener {

	fun onFilterTypeChanged(filterType: ColorFilterType)

	fun onBrightnessChanged(filterType: ColorFilterType)

	fun onSharpnessChanged(filterType: ColorFilterType)

	fun onContrastChanged(filterType: ColorFilterType)
}
