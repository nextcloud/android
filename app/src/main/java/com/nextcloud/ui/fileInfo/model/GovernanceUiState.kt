/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo.model

sealed interface GovernanceUiState {
    data object Loading : GovernanceUiState

    data class Loaded(
        val sensitivityLabels: List<GovernanceLabel>,
        val retentionLabels: List<GovernanceLabel>,
        val holdLabels: List<GovernanceLabel>,
        val currentSensitivityLabelId: String,
        val currentRetentionLabelIds: Set<String>,
        val currentHoldLabelIds: Set<String>
    ) : GovernanceUiState
}
