/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import com.owncloud.android.datamodel.OCFile

sealed interface AudioPlayerScreenEvent {

    data class ShowFileActions(val file: OCFile, val actionsToHide: List<Int>) : AudioPlayerScreenEvent

    data class ShowFileDetails(val file: OCFile) : AudioPlayerScreenEvent

    data object ShowFileExportStartedMessage : AudioPlayerScreenEvent

    data class ShowShareFileDialog(val file: OCFile) : AudioPlayerScreenEvent

    data class ShowRemoveFileDialog(val file: OCFile) : AudioPlayerScreenEvent

    data class LaunchOpenFileIntent(val file: OCFile) : AudioPlayerScreenEvent

    data class LaunchStreamFileIntent(val file: OCFile) : AudioPlayerScreenEvent

    data class ToggleFileLock(val file: OCFile, val shouldBeLocked: Boolean) : AudioPlayerScreenEvent

    data class AddFileToAlbum(val file: OCFile) : AudioPlayerScreenEvent
}
