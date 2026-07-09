/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout
import java.time.Duration
import kotlin.coroutines.CoroutineContext

/**
 * Define a Global Scope for jobs not tied to activities,
 * with a timeout to avoid resource leak
 */
class TimeoutScope(val timeout: Duration, context: CoroutineContext = Dispatchers.Default) {
    private val scope = CoroutineScope(context)
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch {
        withTimeout(timeout, block)
    }
}