/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.tags.model

import androidx.annotation.StringRes
import com.owncloud.android.lib.resources.tags.Tag

sealed interface TagUiState {
    object Loading : TagUiState
    data class Loaded(val allTags: List<Tag>, val assignedTagIds: Set<String>, val query: String = "") : TagUiState
    data class Error(@StringRes val messageId: Int) : TagUiState
}
