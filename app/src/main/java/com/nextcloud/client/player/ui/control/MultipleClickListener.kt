/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.control

import android.os.Handler
import android.os.Looper
import android.view.View
import java.util.Optional

abstract class MultipleClickListener : View.OnClickListener {

    companion object {
        private const val TIME_WINDOW_FOR_CLICK_DETERMINATION_IN_MILLISECONDS = 250L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var clicksCount = Optional.empty<Int>()

    protected abstract fun onSingleClick(view: View?)

    protected abstract fun onDoubleClick(view: View?)

    override fun onClick(view: View?) {
        val interactionIsBegan = clicksCount.isPresent

        if (interactionIsBegan) {
            clicksCount = Optional.of(clicksCount.get() + 1)
        } else {
            clicksCount = Optional.of(1)

            handler.postDelayed({
                val count = clicksCount.get()
                clicksCount = Optional.empty()
                callSubscriber(view, count)
            }, TIME_WINDOW_FOR_CLICK_DETERMINATION_IN_MILLISECONDS)
        }
    }

    private fun callSubscriber(view: View?, clicksCount: Int) {
        if (clicksCount == 1) {
            onSingleClick(view)
        } else {
            onDoubleClick(view)
        }
    }
}
