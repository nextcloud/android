/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.nextcloud.common.NextcloudClient
import com.nextcloud.common.OkHttpMethodBase
import com.owncloud.android.utils.BitmapUtils

private const val INITIALS_FONT_SIZE_RATIO = 0.45f
private const val INITIALS_BACKGROUND_ALPHA = 0.1f

@Composable
fun RemoteAvatar(client: NextcloudClient, path: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = remember(client) { CoilImageLoader.get(context, client) }
    val request = remember(client, path) {
        val headers = NetworkHeaders.Builder()
            .set(OkHttpMethodBase.AUTHORIZATION, client.credentials)
            .build()

        ImageRequest.Builder(context)
            .data(client.baseUri.toString() + path)
            .httpHeaders(headers)
            .build()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        InitialsAvatar(name, size)
        AsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun InitialsAvatar(name: String, size: Dp) {
    val color = remember(name) { BitmapUtils.usernameToColor(name).let { Color(it.r, it.g, it.b) } }
    val fontSize = with(LocalDensity.current) { (size * INITIALS_FONT_SIZE_RATIO).toSp() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            .background(MaterialTheme.colorScheme.surface)
            .background(color.copy(alpha = INITIALS_BACKGROUND_ALPHA))
    ) {
        Text(
            text = remember(name) { BitmapUtils.usernameToInitials(name) },
            color = color,
            fontSize = fontSize,
            maxLines = 1
        )
    }
}
