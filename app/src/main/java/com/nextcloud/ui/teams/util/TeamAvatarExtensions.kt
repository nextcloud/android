/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import android.net.Uri
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.android.lib.resources.teams.model.TeamMember

val Team.avatarPath: String
    get() = "/ocs/v2.php/apps/circles/circles/$id/avatar"

fun TeamMember.avatarPath(sizePx: Int): String? {
    val userId = userId?.takeIf { isUser } ?: return null
    return "/index.php/avatar/${Uri.encode(userId)}/$sizePx"
}
