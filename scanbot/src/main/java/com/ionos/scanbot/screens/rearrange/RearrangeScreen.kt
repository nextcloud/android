/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange

import com.ionos.scanbot.screens.base.BaseScreen
import com.ionos.scanbot.screens.rearrange.recycler.RearrangeItem

internal interface RearrangeScreen {

	data class State(
		val processing: Boolean = false,
		override val event: Event? = null,
	) : BaseScreen.State<Event>

	sealed interface Event : BaseScreen.Event {
		data class ShowItemsEvent(val items: List<RearrangeItem>) : Event
		data class ShowErrorMessageEvent(val error: Throwable) : Event
		object CloseScreenEvent : Event
	}

	interface ViewModel : BaseScreen.ViewModel<Event, State> {

		fun onStart()

		fun onBackPressed()

		fun onPictureDragged(sourcePosition: Int, targetPosition: Int)
	}
}
