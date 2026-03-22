/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import android.net.Uri
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare

const val REMOTE_FILE_SCHEME = "remoteFile"

fun OCFile.getPlaybackUri(): Uri = getPlaybackUri(localId)

fun OCShare.getPlaybackUri(): Uri = getPlaybackUri(fileSource)

fun getPlaybackUri(fileId: Long): Uri = Uri.Builder()
    .scheme(REMOTE_FILE_SCHEME)
    .authority("")
    .appendPath(fileId.toString())
    .build()

fun Uri.getRemoteFileId(): Long? = scheme
    ?.takeIf { it == REMOTE_FILE_SCHEME }
    ?.let { pathSegments.firstOrNull()?.toLongOrNull() }
