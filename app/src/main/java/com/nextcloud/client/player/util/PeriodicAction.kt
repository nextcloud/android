/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.util

import android.os.Handler
import android.os.Looper

class PeriodicAction(private val periodicIntervalInMilliseconds: Long, private val action: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = Runnable {
        action.invoke()
        start()
    }

    fun start() {
        stop()
        handler.postDelayed(runnable, periodicIntervalInMilliseconds)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
    }
}
