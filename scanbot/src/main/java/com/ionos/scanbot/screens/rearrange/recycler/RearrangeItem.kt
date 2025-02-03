/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange.recycler

import com.ionos.scanbot.entity.Picture

internal data class RearrangeItem(
	val picture: Picture,
	val sequenceNumber: Int,
)
