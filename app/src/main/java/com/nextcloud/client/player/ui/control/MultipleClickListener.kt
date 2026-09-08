/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.control

import android.os.Handler
import android.os.Looper
import android.view.View

private const val TIME_WINDOW_FOR_CLICK_DETERMINATION_IN_MILLISECONDS = 250L
private const val SINGLE_CLICK_COUNT = 1

class MultipleClickListener(private val onSingleClick: () -> Unit, private val onDoubleClick: () -> Unit) :
    View.OnClickListener {

    private val handler = Handler(Looper.getMainLooper())
    private var clicksCount: Int? = null

    override fun onClick(view: View?) {
        val pendingClicksCount = clicksCount
        if (pendingClicksCount != null) {
            clicksCount = pendingClicksCount + 1
            return
        }

        clicksCount = SINGLE_CLICK_COUNT
        handler.postDelayed({
            val count = clicksCount ?: SINGLE_CLICK_COUNT
            clicksCount = null
            if (count == SINGLE_CLICK_COUNT) {
                onSingleClick()
            } else {
                onDoubleClick()
            }
        }, TIME_WINDOW_FOR_CLICK_DETERMINATION_IN_MILLISECONDS)
    }
}
