/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.entity

import android.graphics.PointF


internal data class SelectedContour(val normalizedPolygon: List<PointF>) {
	companion object {
		val DEFAULT: SelectedContour = SelectedContour(
			normalizedPolygon = listOf(
				PointF(0.0F, 0.0F),
				PointF(1.0F, 0.0F),
				PointF(1.0F, 1.0F),
				PointF(0.0F, 1.0F),
			),
		)
	}
}