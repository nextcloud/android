/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.ShareEntity
import com.owncloud.android.lib.resources.shares.OCShare

fun OCShare.hasFileRequestPermission(): Boolean = (isFolder && shareType?.isPublicOrMail() == true)

fun List<OCShare>.mergeDistinctByToken(other: List<OCShare>): List<OCShare> = (this + other).distinctBy { it.token }

fun OCShare.toEntity(accountName: String): ShareEntity = ShareEntity(
    id = remoteId.toInt(), // so that db is not keep updating same files
    idRemoteShared = remoteId.toInt(),
    path = path,
    itemSource = itemSource.toInt(),
    fileSource = fileSource.toInt(),
    shareType = shareType?.value,
    shareWith = shareWith,
    permissions = permissions,
    sharedDate = sharedDate.toInt(),
    expirationDate = expirationDate.toInt(),
    token = token,
    shareWithDisplayName = sharedWithDisplayName,
    isDirectory = if (isFolder) 1 else 0,
    userId = userId,
    accountOwner = accountName,
    isPasswordProtected = if (isPasswordProtected) 1 else 0,
    note = note,
    hideDownload = if (isHideFileDownload) 1 else 0,
    shareLink = shareLink,
    shareLabel = label,
    attributes = attributes,
    downloadLimitLimit = fileDownloadLimit?.limit,
    downloadLimitCount = fileDownloadLimit?.count
)
