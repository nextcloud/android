/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import android.view.View
import androidx.annotation.OptIn
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
fun PlayerView.applyControlsInsets(insets: Insets) {
    val controller = findViewById<View>(androidx.media3.ui.R.id.exo_controller) ?: return
    controller.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
}

@OptIn(UnstableApi::class)
fun PlayerView.setFullscreenButton(isFullscreen: Boolean, onClick: () -> Unit) {
    setFullscreenButtonClickListener(null)
    setFullscreenButtonState(isFullscreen)
    setFullscreenButtonClickListener { onClick() }
}
