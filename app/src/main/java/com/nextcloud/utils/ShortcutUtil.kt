/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Felix Nüsse <felix.nuesse@t-online.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class ShortcutUtil @Inject constructor(private val mContext: Context) {

    /**
     * Adds a pinned shortcut to the home screen that points to the passed file/folder.
     *
     * @param file The file/folder to which a pinned shortcut should be added to the home screen.
     */
    fun addShortcutToHomescreen(
        file: OCFile,
        viewThemeUtils: ViewThemeUtils,
        user: User,
        syncedFolderProvider: SyncedFolderProvider
    ) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(mContext)) {
            return
        }

        val intent = Intent(mContext, FileDisplayActivity::class.java).apply {
            action = FileDisplayActivity.OPEN_FILE
            putExtra(FileActivity.EXTRA_FILE_REMOTE_PATH, file.decryptedRemotePath)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val icon = createShortcutIcon(file, viewThemeUtils, user, syncedFolderProvider)

        val shortcutInfo = ShortcutInfoCompat.Builder(mContext, "nextcloud_shortcut_${file.remoteId}")
            .setShortLabel(file.fileName)
            .setLongLabel(mContext.getString(R.string.pin_shortcut_label, file.fileName))
            .setIcon(icon)
            .setIntent(intent)
            .build()

        val resultIntent =
            ShortcutManagerCompat.createShortcutResultIntent(mContext, shortcutInfo)

        val pendingIntent = PendingIntent.getBroadcast(
            mContext,
            file.hashCode(),
            resultIntent,
            FLAG_IMMUTABLE
        )

        ShortcutManagerCompat.requestPinShortcut(mContext, shortcutInfo, pendingIntent.intentSender)
    }

    private fun createShortcutIcon(
        file: OCFile,
        viewThemeUtils: ViewThemeUtils,
        user: User,
        syncedFolderProvider: SyncedFolderProvider
    ): IconCompat {
        val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
        )

        return when {
            thumbnail != null -> IconCompat.createWithAdaptiveBitmap(bitmapToAdaptiveBitmap(thumbnail))

            file.isFolder -> {
                val isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user)
                val isDarkModeActive = syncedFolderProvider.preferences.isDarkModeEnabled
                val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
                val drawable = MimeTypeUtil.getFolderIcon(isDarkModeActive, overlayIconId, mContext, viewThemeUtils)
                IconCompat.createWithBitmap(drawable.toBitmap())
            }

            else -> IconCompat.createWithResource(
                mContext,
                MimeTypeUtil.getFileTypeIconId(file.mimeType, file.fileName)
            )
        }
    }

    private fun bitmapToAdaptiveBitmap(orig: Bitmap): Bitmap {
        val adaptiveIconSize = mContext.resources.getDimensionPixelSize(R.dimen.adaptive_icon_size)
        val adaptiveIconOuterSides = mContext.resources.getDimensionPixelSize(R.dimen.adaptive_icon_padding)
        val drawable: Drawable = orig.toDrawable(mContext.resources)
        val bitmap = createBitmap(adaptiveIconSize, adaptiveIconSize)
        val canvas = Canvas(bitmap)
        drawable.setBounds(
            adaptiveIconOuterSides,
            adaptiveIconOuterSides,
            adaptiveIconSize - adaptiveIconOuterSides,
            adaptiveIconSize - adaptiveIconOuterSides
        )
        drawable.draw(canvas)
        return bitmap
    }
}
