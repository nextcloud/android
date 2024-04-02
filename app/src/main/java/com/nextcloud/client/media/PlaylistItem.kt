/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.media

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

data class PlaylistItem(val file: OCFile, val startPositionMs: Long, val autoPlay: Boolean, val user: User)
