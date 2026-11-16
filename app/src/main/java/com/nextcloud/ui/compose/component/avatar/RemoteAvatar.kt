/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.avatar

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nextcloud.common.NextcloudClient
import com.nextcloud.common.OkHttpMethodBase
import com.owncloud.android.ui.TextDrawable

@Composable
fun RemoteAvatar(client: NextcloudClient?, path: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val initials = remember(name, sizePx) {
        val bitmap = TextDrawable.createNamedAvatar(name, sizePx / 2f).toBitmap(sizePx, sizePx)
        BitmapPainter(bitmap.asImageBitmap())
    }
    val request = remember(client, path) {
        client?.let {
            val headers = NetworkHeaders.Builder()
                .set(OkHttpMethodBase.AUTHORIZATION, it.credentials)
                .build()

            ImageRequest.Builder(context)
                .data(it.baseUri.toString() + path)
                .httpHeaders(headers)
                .crossfade(true)
                .build()
        }
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        placeholder = initials,
        error = initials,
        fallback = initials,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    )
}
