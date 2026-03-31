/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation

import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.ui.fragment.ActivitiesFragment
import com.owncloud.android.ui.fragment.community.CommunityFragment
import kotlinx.parcelize.Parcelize

sealed class NavigatorScreen(val tag: String) : Parcelable {

    @Parcelize
    object Activities : NavigatorScreen("Activities")

    @Parcelize
    object Community : NavigatorScreen("Community")

    companion object {
        fun fromTag(tag: String?): NavigatorScreen? = when (tag) {
            "Activities" -> Activities
            "Community" -> Community
            else -> null
        }
    }

    fun menuItemId(): Int = when (this) {
        Community -> R.id.nav_community
        Activities -> R.id.nav_activity
    }

    fun actionBarStyle(): Pair<ActionBarStyle, Int> = when (this) {
        Community -> ActionBarStyle.Plain to R.string.drawer_community
        Activities -> ActionBarStyle.Plain to R.string.drawer_item_activities
    }

    fun toFragment(): Fragment = when (this) {
        Community -> CommunityFragment()
        Activities -> ActivitiesFragment()
    }
}
