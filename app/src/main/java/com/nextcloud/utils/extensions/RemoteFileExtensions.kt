/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.tags.Tag

fun RemoteFile.sharedViaLink(): Boolean = sharees?.any { it.shareType?.isLink == true } ?: false

fun RemoteFile.sharedWithSharee(): Boolean = sharees?.isNotEmpty() ?: false

fun RemoteFile.getShareeList(): List<ShareeUser> = sharees?.toList() ?: emptyList()

fun RemoteFile.tags(): List<Tag> = tags?.mapNotNull { it } ?: emptyList()
