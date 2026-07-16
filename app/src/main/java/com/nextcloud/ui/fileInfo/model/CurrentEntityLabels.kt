/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo.model

data class CurrentEntityLabels(
    val sensitivityId: String = "",
    val retentionIds: Set<String> = emptySet(),
    val holdIds: Set<String> = emptySet()
)
