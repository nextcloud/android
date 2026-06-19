/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.tags.model

import com.owncloud.android.lib.resources.tags.Tag

sealed interface TagUiState {
    object Loading : TagUiState
    data class Loaded(val allTags: List<Tag>, val assignedTagIds: Set<String>, val query: String = "") : TagUiState

    data class Error(val message: String) : TagUiState
}
