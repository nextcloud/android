/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.pager

import androidx.fragment.app.Fragment

interface PlayerPagerFragmentFactory<T> {
    fun create(item: T): Fragment
}
