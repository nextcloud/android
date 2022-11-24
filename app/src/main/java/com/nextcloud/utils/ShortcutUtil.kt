/*
 * Nextcloud Android client application
 *
 * @author Felix Nüsse
 *
 * Copyright (C) 2022 Felix Nüsse
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.utils

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
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
    fun addShortcutToHomescreen(file: OCFile, viewThemeUtils: ViewThemeUtils) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(mContext)) {
            val intent = Intent(mContext, FileDisplayActivity::class.java)
            intent.action = FileDisplayActivity.OPEN_FILE
            intent.putExtra(FileActivity.EXTRA_FILE, file.remotePath)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val shortcutId = "nextcloud_shortcut_" + file.remoteId
            val icon: IconCompat
            var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
            )
            if (thumbnail != null) {
                thumbnail = bitmapToAdaptiveBitmap(thumbnail)
                icon = IconCompat.createWithAdaptiveBitmap(thumbnail)
            } else if (file.isFolder) {
                val bitmapIcon = MimeTypeUtil.getFolderTypeIcon(
                    file.isSharedWithMe || file.isSharedWithSharee,
                    file.isSharedViaLink,
                    file.isEncrypted,
                    file.isGroupFolder,
                    file.mountType,
                    mContext,
                    viewThemeUtils
                ).toBitmap()
                icon = IconCompat.createWithBitmap(bitmapIcon)
            } else {
                icon = IconCompat.createWithResource(
                    mContext,
                    MimeTypeUtil.getFileTypeIconId(file.mimeType, file.fileName)
                )
            }
            val longLabel = mContext.getString(R.string.pin_shortcut_label, file.fileName)
            val pinShortcutInfo = ShortcutInfoCompat.Builder(mContext, shortcutId)
                .setShortLabel(file.fileName)
                .setLongLabel(longLabel)
                .setIcon(icon)
                .setIntent(intent)
                .build()
            val pinnedShortcutCallbackIntent =
                ShortcutManagerCompat.createShortcutResultIntent(mContext, pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                mContext,
                0,
                pinnedShortcutCallbackIntent,
                FLAG_IMMUTABLE
            )
            ShortcutManagerCompat.requestPinShortcut(
                mContext,
                pinShortcutInfo,
                successCallback.intentSender
            )
        }
    }

    private fun bitmapToAdaptiveBitmap(orig: Bitmap): Bitmap {
        val adaptiveIconSize = mContext.resources.getDimensionPixelSize(R.dimen.adaptive_icon_size)
        val adaptiveIconOuterSides = mContext.resources.getDimensionPixelSize(R.dimen.adaptive_icon_padding)
        val drawable: Drawable = BitmapDrawable(mContext.resources, orig)
        val bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888)
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
