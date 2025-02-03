/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery.pager

import android.widget.ImageView
import com.ionos.scanbot.entity.Picture

internal class GalleryPagerItem internal constructor(
	val picture: Picture,
	val imageView: ImageView,
)
