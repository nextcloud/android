/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.model

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WorkerStateObserver {
    private const val BUFFER_CAPACITY = 25

    private val _events = MutableSharedFlow<WorkerState>(extraBufferCapacity = BUFFER_CAPACITY)
    val events = _events.asSharedFlow()

    fun send(state: WorkerState) {
        _events.tryEmit(state)
    }
}
