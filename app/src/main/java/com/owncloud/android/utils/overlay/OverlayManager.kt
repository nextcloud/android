/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.overlay

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
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

    /**
     * Sets the overlay icon for a folder into the provided [ImageView].
     *
     * The icon is only applied when:
     * - The [folder] is not null
     * - The [folder] represents a directory
     * - A valid overlay icon resource can be resolved
     *
     * The overlay icon depends on whether the folder is configured
     * as an auto-upload folder for the current user.
     *
     * @param folder The [OCFile] representing the folder.
     * @param imageView The [ImageView] where the overlay icon will be displayed.
     */
    fun setFolderOverlayIcon(folder: OCFile?, imageView: ImageView) {
        val overlayIconId = folder
            ?.takeIf { it.isFolder }
            ?.let { currentFolder ->
                val isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(
                    syncedFolderProvider,
                    currentFolder,
                    accountManager.user
                )
                currentFolder.getFileOverlayIconId(isAutoUploadFolder)
            }

        if (overlayIconId == null) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(ContextCompat.getDrawable(context, overlayIconId))
        }
    }

    /**
     * Sets the thumbnail for a folder into the provided [ImageView].
     *
     * This method:
     * - Ensures the given [folder] is not null and represents a directory.
     * - Stops any active shimmer/loading animation on [loaderImageView].
     * - Resolves whether the folder is configured as an auto-upload folder
     *   for the current user.
     * - Detects whether dark mode is currently enabled.
     * - Retrieves the appropriate folder icon and overlay.
     *
     * The final drawable is created via `MimeTypeUtil.getFolderIcon(...)`,
     * which returns a LayerDrawable. This drawable is built programmatically
     * by stacking multiple layers (e.g., base folder icon + optional overlay icon)
     * on top of each other, so everything is rendered inside a single [ImageView].
     *
     * @param folder The [OCFile] representing the folder.
     * @param imageView The [ImageView] where the composed folder thumbnail
     * will be displayed.
     * @param loaderImageView Optional [LoaderImageView] used for shimmer/loading
     * state handling. If provided, its shimmer animation will be stopped before
     * applying the final icon.
     */
    fun setFolderThumbnail(folder: OCFile?, imageView: ImageView, loaderImageView: LoaderImageView?) {
        if (folder == null || !folder.isFolder) return

        DisplayUtils.stopShimmer(loaderImageView, imageView)

        val isAutoUploadFolder =
            SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, folder, accountManager.user)
        val isDarkModeActive = preferences.isDarkModeEnabled()

        val overlayIconId = folder.getFileOverlayIconId(isAutoUploadFolder)
        val icon = MimeTypeUtil.getFolderIcon(isDarkModeActive, overlayIconId, context, viewThemeUtils)
        imageView.setImageDrawable(icon)
    }
}
