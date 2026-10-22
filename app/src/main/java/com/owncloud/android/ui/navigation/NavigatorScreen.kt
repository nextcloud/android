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
import com.owncloud.android.ui.fragment.autoUpload.AutoUploadFragment
import com.owncloud.android.ui.fragment.uploadList.UploadListFragment
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
    object AutoUpload : NavigatorScreen(AUTO_UPLOAD_TAG, hasDrawer = false)

    @Parcelize
    object Trashbin : NavigatorScreen(TRASHBIN_TAG)

    @Parcelize
    object UploadList : NavigatorScreen(UPLOAD_LIST_TAG)

    companion object {
        private const val TRASHBIN_TAG = "Trashbin"
        private const val ACTIVITIES_TAG = "Activities"
        private const val COMMUNITY_TAG = "Community"
        private const val NOTIFICATIONS_TAG = "Notifications"
        private const val UPLOAD_LIST_TAG = "UploadList"
        private const val AUTO_UPLOAD_TAG = "AutoUpload"

        fun fromTag(tag: String?): NavigatorScreen? = when (tag) {
            ACTIVITIES_TAG -> Activities
            COMMUNITY_TAG -> Community
            NOTIFICATIONS_TAG -> Notifications
            TRASHBIN_TAG -> Trashbin
            UPLOAD_LIST_TAG -> UploadList
            AUTO_UPLOAD_TAG -> AutoUpload
            else -> null
        }
    }

    fun menuItemId(): Int = when (this) {
        Community -> R.id.nav_community
        Activities -> R.id.nav_activity
        Trashbin -> R.id.nav_trashbin
        UploadList -> R.id.nav_uploads
        Notifications -> -1
        AutoUpload -> R.id.nav_settings
    }

    fun actionBarStyle(): Pair<ActionBarStyle, Int> = when (this) {
        Community -> ActionBarStyle.Plain to R.string.drawer_community
        Activities -> ActionBarStyle.Plain to R.string.drawer_item_activities
        Trashbin -> ActionBarStyle.Plain to R.string.drawer_item_trashbin
        UploadList -> ActionBarStyle.Plain to R.string.uploads_view_title
        Notifications -> ActionBarStyle.Plain to R.string.drawer_item_notifications
        AutoUpload -> ActionBarStyle.Plain to R.string.drawer_synced_folders
    }

    fun toFragment(): Fragment = when (this) {
        Community -> CommunityFragment()
        Activities -> ActivitiesFragment()
        Notifications -> NotificationsFragment()
        Trashbin -> TrashbinFragment()
        UploadList -> UploadListFragment()
        AutoUpload -> AutoUploadFragment()
    }
}
