/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

import androidx.annotation.StringRes

sealed interface TeamDetailListUiState<out T> {
    data object Loading : TeamDetailListUiState<Nothing>

    data class Error(@StringRes val messageId: Int) : TeamDetailListUiState<Nothing>

    data class Content<T>(val items: List<T>) : TeamDetailListUiState<T>
}
