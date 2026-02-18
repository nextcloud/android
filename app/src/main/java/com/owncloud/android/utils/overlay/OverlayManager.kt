/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.overlay

import android.content.Context
import android.widget.ImageView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class OverlayManager @Inject constructor(
    private val syncedFolderProvider: SyncedFolderProvider,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val context: Context,
    private val accountManager: UserAccountManager
) : Injectable {

    fun setFolderThumbnail(folder: OCFile?, imageView: ImageView, loaderImageView: LoaderImageView?) {
        if (folder == null) {
            return
        }

        if (!folder.isFolder) {
            return
        }

        DisplayUtils.stopShimmer(loaderImageView, imageView)

        val isAutoUploadFolder =
            SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, folder, accountManager.user)
        val isDarkModeActive = preferences.isDarkModeEnabled()

        val overlayIconId = folder.getFileOverlayIconId(isAutoUploadFolder)
        val icon = MimeTypeUtil.getFolderIcon(isDarkModeActive, overlayIconId, context, viewThemeUtils)
        imageView.setImageDrawable(icon)
    }
}
