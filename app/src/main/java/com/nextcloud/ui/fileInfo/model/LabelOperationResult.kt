/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo.model

sealed interface LabelOperationResult {
    data object Success : LabelOperationResult

    data object Forbidden : LabelOperationResult

    data object Failure : LabelOperationResult
}
