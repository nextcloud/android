/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.files

import com.owncloud.android.R

enum class DeepLinkConstants(val route: String, val navId: Int) {
    OPEN_FILES("openFiles", R.id.nav_all_files),
    OPEN_FAVORITES("openFavorites", R.id.nav_favorites),
    OPEN_MEDIA("openMedia", R.id.nav_gallery),
    OPEN_SHARED("openShared", R.id.nav_shared),
    OPEN_OFFLINE("openOffline", R.id.nav_on_device),
    OPEN_NOTIFICATIONS("openNotifications", R.id.nav_notifications),
    OPEN_DELETED("openDeleted", R.id.nav_trashbin),
    OPEN_SETTINGS("openSettings", R.id.nav_settings),

    // Special case, handled separately
    OPEN_AUTO_UPLOAD("openAutoUpload", -1),
    OPEN_EXTERNAL_URL("openUrl", -1),
    ACTION_CREATE_NEW("createNew", -1),
    ACTION_APP_UPDATE("checkAppUpdate", -1);

    companion object {
        fun fromPath(path: String?): DeepLinkConstants? {
            return entries.find { it.route == path }
        }

        val navigationPaths = entries.map { it.route }
    }
}
