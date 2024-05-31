/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.files

// deep link url format should be: https://example.com/app/<constants>
object DeepLinkConstants {
    const val OPEN_FILES = "openFiles"
    const val OPEN_FAVORITES = "openFavorites"
    const val OPEN_MEDIA = "openMedia"
    const val OPEN_SHARED = "openShared"
    const val OPEN_OFFLINE = "openOffline"
    const val OPEN_NOTIFICATIONS = "openNotifications"
    const val OPEN_DELETED = "openDeleted"
    const val OPEN_SETTINGS = "openSettings"
    const val OPEN_AUTO_UPLOAD = "openAutoUpload"

    // for external url the url should have the following format
    // https://example.com/app/openUrl?url=<url_to_open>
    const val OPEN_EXTERNAL_URL = "openUrl"

    const val ACTION_CREATE_NEW = "createNew"
    const val ACTION_APP_UPDATE = "checkAppUpdate"
}