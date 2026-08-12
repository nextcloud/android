/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.res.Configuration
import android.content.res.Resources

fun Resources.isLandscape(): Boolean = configuration.isLandscape()

fun Configuration.isLandscape(): Boolean = orientation == Configuration.ORIENTATION_LANDSCAPE
