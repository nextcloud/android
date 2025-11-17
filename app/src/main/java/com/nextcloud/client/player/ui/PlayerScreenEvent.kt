/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui

import com.owncloud.android.datamodel.OCFile

sealed interface PlayerScreenEvent {

    data class ShowFileActions(val file: OCFile, val actionIds: List<Int>) : PlayerScreenEvent

    data class ShowFileDetails(val file: OCFile) : PlayerScreenEvent

    data object ShowFileExportStartedMessage : PlayerScreenEvent

    data class ShowShareFileDialog(val file: OCFile) : PlayerScreenEvent

    data class ShowRemoveFileDialog(val file: OCFile) : PlayerScreenEvent

    data class LaunchOpenFileIntent(val file: OCFile) : PlayerScreenEvent

    data class LaunchStreamFileIntent(val file: OCFile) : PlayerScreenEvent
}
