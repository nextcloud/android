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

package com.owncloud.android.utils

import com.owncloud.android.datamodel.OCFile
import android.content.pm.ShortcutManager
import android.content.Intent
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import android.content.pm.ShortcutInfo
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build

object ShortcutUtil {

    /**
     * Adds a pinned shortcut to the home screen that points to the passed file/folder.
     *
     * @param file The file/folder to which a pinned shortcut should be added to the home screen.
     */
    fun addShortcutToHomescreen(context: Context, file: OCFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                val intent = Intent(context, FileDisplayActivity::class.java)
                intent.action = FileDisplayActivity.OPEN_FILE
                intent.putExtra(FileActivity.EXTRA_FILE, file.remotePath)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val shortcutId = "nextcloud_shortcut_" + file.remoteId
                val icon: Icon
                var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
                )
                if (thumbnail != null) {
                    thumbnail = bitmapToAdaptiveBitmap(thumbnail, context)
                    icon = Icon.createWithAdaptiveBitmap(thumbnail)
                } else if (file.isFolder) {
                    icon = Icon.createWithResource(
                        context,
                        MimeTypeUtil.getFolderTypeIconId(
                            file.isSharedWithMe ||
                                file.isSharedWithSharee, file.isSharedViaLink, file.isEncrypted, file.mountType
                        )
                    )
                } else {
                    icon = Icon.createWithResource(
                        context,
                        MimeTypeUtil.getFileTypeIconId(file.mimeType, file.fileName)
                    )
                }
                val pinShortcutInfo = ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(file.fileName)
                    .setLongLabel("Open " + file.fileName)
                    .setIcon(icon)
                    .setIntent(intent)
                    .build()
                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(
                    context, 0,
                    pinnedShortcutCallbackIntent, 0
                )
                shortcutManager.requestPinShortcut(
                    pinShortcutInfo,
                    successCallback.intentSender
                )
            }
        }
    }

    private fun bitmapToAdaptiveBitmap(orig: Bitmap, context: Context): Bitmap {
        val screenDensity = context.resources.displayMetrics.density
        val adaptiveIconSize = Math.round(108 * screenDensity)
        val adaptiveIconOuterSides = Math.round(18 * screenDensity)
        val drawable: Drawable = BitmapDrawable(context.resources, orig)
        val bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(
            adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides,
            adaptiveIconSize - adaptiveIconOuterSides
        )
        drawable.draw(canvas)
        return bitmap
    }
}