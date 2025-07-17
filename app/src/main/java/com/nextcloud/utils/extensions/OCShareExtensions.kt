/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.resources.shares.OCShare

fun OCShare.hasFileRequestPermission(): Boolean = (isFolder && shareType?.isPublicOrMail() == true)

fun List<OCShare>.mergeDistinctByToken(other: List<OCShare>): List<OCShare> = (this + other).distinctBy { it.token }
