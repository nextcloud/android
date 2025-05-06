/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType

fun OCShare.hasFileRequestPermission(): Boolean {
    return (isFolder && (shareType == ShareType.PUBLIC_LINK || shareType == ShareType.EMAIL))
}
