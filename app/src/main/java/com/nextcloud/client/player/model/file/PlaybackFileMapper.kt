/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

fun OCFile.toPlaybackFile() = PlaybackFile(
    id = localId.toString(),
    uri = getPlaybackUri().toString(),
    name = fileName,
    mimeType = mimeType,
    contentLength = fileLength,
    lastModified = modificationTimestamp,
    isFavorite = isFavorite
)

fun OCShare.toPlaybackFile() = PlaybackFile(
    id = fileSource.toString(),
    uri = getPlaybackUri().toString(),
    name = path?.let { File(it).name } ?: "",
    mimeType = getMimeType(),
    contentLength = -1L,
    lastModified = sharedDate * 1000L,
    isFavorite = isFavorite
)

private fun OCShare.getMimeType(): String = mimetype
    ?.takeIf { it.isNotEmpty() }
    ?: path?.let { MimeTypeUtil.getMimeTypeFromPath(it) }
    ?: ""
