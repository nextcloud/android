/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.config

import android.content.res.Configuration

private const val DEFAULT_FONT_SCALE = 1.0f

internal fun Configuration.applyDefaultFontScale() {
    fontScale = DEFAULT_FONT_SCALE
}
