/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.resources.shares.OCShare

fun OCShare.hasFileRequestPermission(): Boolean {
    return (isFolder && shareType?.isPublicOrMail() == true)
}

fun List<OCShare>.mergeDistinctByToken(other: List<OCShare>): List<OCShare> {
    return (this + other).distinctBy { it.token }
}
