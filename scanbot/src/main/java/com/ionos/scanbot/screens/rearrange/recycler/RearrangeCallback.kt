/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange.recycler

import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder

internal class RearrangeCallback(
	private val onItemDragged: (sourcePosition: Int, targetPosition: Int) -> Unit,
) : SimpleCallback(UP or DOWN or START or END, 0) {

	override fun onMove(recyclerView: RecyclerView, source: ViewHolder, target: ViewHolder): Boolean {
		val sourcePosition = source.bindingAdapterPosition
		val targetPosition = target.bindingAdapterPosition
		if (sourcePosition != NO_POSITION && targetPosition != NO_POSITION) {
			onItemDragged.invoke(sourcePosition, targetPosition)
			return true
		}
		return false
	}

	override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}
}
