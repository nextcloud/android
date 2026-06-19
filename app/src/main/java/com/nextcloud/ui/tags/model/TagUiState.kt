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
    object Error : TagUiState
}

fun List<Tag>.toLoaded(currentTags: List<Tag>): TagUiState {
    val assignedTagNames = currentTags.map { it.name }.toSet()

    val assignedIds = this
        .filter { it.name in assignedTagNames }
        .map { it.id }
        .toSet()

    return TagUiState.Loaded(this, assignedIds)
}
