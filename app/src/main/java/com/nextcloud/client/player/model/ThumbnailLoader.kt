/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.nextcloud.client.player.model.file.PlaybackFile
import java.util.concurrent.Future

interface ThumbnailLoader {

    suspend fun await(context: Context, file: PlaybackFile, width: Int, height: Int): Bitmap?

    fun load(context: Context, file: PlaybackFile, width: Int, height: Int): Future<Bitmap>

    fun load(context: Context, model: Any, fileId: String?, width: Int, height: Int): Future<Bitmap>

    fun load(imageView: ImageView, model: Any, fileId: String)
}
