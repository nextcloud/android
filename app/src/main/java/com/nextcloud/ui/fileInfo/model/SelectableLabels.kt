/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo.model

data class SelectableLabels(
    val sensitivityLabels: List<GovernanceLabel>,
    val retentionLabels: List<GovernanceLabel>,
    val holdLabels: List<GovernanceLabel>
)
