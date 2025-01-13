/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import com.owncloud.android.BuildConfig

object BuildHelper {
    const val GPLAY: String = "gplay"

    fun isFlavourGPlay(): Boolean = GPLAY == BuildConfig.FLAVOR
}
