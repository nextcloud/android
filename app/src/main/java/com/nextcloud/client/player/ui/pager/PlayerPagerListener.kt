/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.pager

fun interface PlayerPagerListener<T> {
    fun onSwitchToItem(item: T)
}
