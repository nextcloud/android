/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

interface MediaNavigator {

    val hasNext: Boolean

    val hasPrevious: Boolean

    fun showNext()

    fun showPrevious()
}
