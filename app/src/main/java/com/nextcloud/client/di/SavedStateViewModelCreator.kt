/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

fun interface SavedStateViewModelCreator {
    fun create(savedStateHandle: SavedStateHandle): ViewModel
}
