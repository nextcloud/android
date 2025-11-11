/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui.composeActivity

sealed class ComposeDestination(val id: Int) {
    data class AssistantScreen(val sessionId: Long?) : ComposeDestination(0)

    companion object {
        fun fromId(id: Int): ComposeDestination {
            return when (id) {
                0 -> AssistantScreen(null)
                else -> throw IllegalArgumentException("Unknown destination: $id")
            }
        }
    }
}
