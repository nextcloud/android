/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.filter

import android.graphics.Bitmap

internal interface Filterable {

	fun getCropFilter(): Filter

	fun getColorFilter(): Filter

	fun getRotateFilter(): Filter

	fun applyFilters(filterTypes: Set<FilterType>, bitmap: Bitmap): Bitmap?
}