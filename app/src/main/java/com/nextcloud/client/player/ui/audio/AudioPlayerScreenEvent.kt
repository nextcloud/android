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

    data object ShowFileExportStartedMessage : AudioPlayerScreenEvent
}
