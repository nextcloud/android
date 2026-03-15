/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.sendButton

import android.graphics.drawable.Drawable

data class SendButtonData(
    @JvmField val drawable: Drawable?,
    @JvmField val title: CharSequence?,
    val packageName: String?,
    val activityName: String?
)
