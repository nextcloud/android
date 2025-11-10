/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.composeActivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object ComposeNavigation {
    private var root: MutableStateFlow<ComposeDestination?> = MutableStateFlow(null)
    val currentScreen: StateFlow<ComposeDestination?> = root
    fun navigate(value: ComposeDestination) = root.update { value }
}
