/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

import com.nextcloud.android.lib.resources.teams.post.members.model.NewTeamMember

data class TeamMemberSuggestion(val label: String, val member: NewTeamMember)
