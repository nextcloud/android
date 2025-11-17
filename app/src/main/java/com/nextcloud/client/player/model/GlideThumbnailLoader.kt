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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.signature.ObjectKey
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.player.model.file.PlaybackFile
import com.owncloud.android.MainApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Future
import javax.inject.Inject
import kotlin.coroutines.resume

class GlideThumbnailLoader @Inject constructor(clientFactory: ClientFactory, userAccountManager: UserAccountManager) :
    ThumbnailLoader {
    private val client by lazy { clientFactory.createNextcloudClient(userAccountManager.user) }

    override suspend fun await(context: Context, file: PlaybackFile, width: Int, height: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    val future = load(context, file, width, height)
                    continuation.invokeOnCancellation { future.cancel(true) }
                    continuation.resume(future.get())
                }.onFailure {
                    if (it is CancellationException) throw it
                    continuation.resume(null)
                }
            }
        }

    override fun load(context: Context, file: PlaybackFile, width: Int, height: Int): Future<Bitmap> {
        val url = createUrl(file, width, height)
        return load(context, url, file.id, width, height)
    }

    override fun load(context: Context, model: Any, fileId: String?, width: Int, height: Int): Future<Bitmap> = Glide
        .with(context)
        .asBitmap()
        .load(model)
        .signature(ObjectKey(fileId ?: model.toString()))
        .submit(width, height)

    override fun load(imageView: ImageView, model: Any, fileId: String) {
        Glide
            .with(imageView)
            .load(model)
            .signature(ObjectKey(fileId))
            .into(imageView)
    }

    private fun createUrl(file: PlaybackFile, width: Int, height: Int) = GlideUrl(
        "${client.baseUri}/index.php/core/preview?fileId=${file.id}&x=$width&y=$height&a=1&mode=cover&forceIcon=0",
        LazyHeaders.Builder()
            .addHeader("Authorization", client.credentials)
            .addHeader("User-Agent", MainApp.getUserAgent())
            .build()
    )
}
