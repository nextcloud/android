/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

enum class TeamResourceProviderId(val id: String) {
    FILES("files"),
    TEAM_FOLDER("groupfolders"),
    TALK("talk"),
    DECK("deck"),
    COLLECTIVES("collectives");

    companion object {
        fun from(id: String): TeamResourceProviderId? = entries.firstOrNull { it.id == id }
    }
}
