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
import com.owncloud.android.ui.fragment.notifications.NotificationsFragment
import com.owncloud.android.ui.navigation.model.ActionBarStyle
import com.owncloud.android.ui.trashbin.TrashbinFragment
import kotlinx.parcelize.Parcelize

sealed class NavigatorScreen(val tag: String, val hasDrawer: Boolean = true) : Parcelable {

    @Parcelize
    object Activities : NavigatorScreen(ACTIVITIES_TAG)

    @Parcelize
    object Community : NavigatorScreen(COMMUNITY_TAG)

    @Parcelize
    object Notifications : NavigatorScreen(NOTIFICATIONS_TAG, hasDrawer = false)

    @Parcelize
    object Trashbin : NavigatorScreen(TRASHBIN_TAG)

    companion object {
        private const val TRASHBIN_TAG = "Trashbin"
        private const val ACTIVITIES_TAG = "Activities"
        private const val COMMUNITY_TAG = "Community"
        private const val NOTIFICATIONS_TAG = "Notifications"

        fun fromTag(tag: String?): NavigatorScreen? = when (tag) {
            ACTIVITIES_TAG -> Activities
            COMMUNITY_TAG -> Community
            NOTIFICATIONS_TAG -> Notifications
            TRASHBIN_TAG -> Trashbin
            else -> null
        }
    }

    fun menuItemId(): Int = when (this) {
        Community -> R.id.nav_community
        Activities -> R.id.nav_activity
        Trashbin -> R.id.nav_trashbin
        Notifications -> -1
    }

    fun actionBarStyle(): Pair<ActionBarStyle, Int> = when (this) {
        Community -> ActionBarStyle.Plain to R.string.drawer_community
        Activities -> ActionBarStyle.Plain to R.string.drawer_item_activities
        Trashbin -> ActionBarStyle.Plain to R.string.drawer_item_trashbin
        Notifications -> ActionBarStyle.Plain to R.string.drawer_item_notifications
    }

    fun toFragment(): Fragment = when (this) {
        Community -> CommunityFragment()
        Activities -> ActivitiesFragment()
        Notifications -> NotificationsFragment()
        Trashbin -> TrashbinFragment()
    }
}
