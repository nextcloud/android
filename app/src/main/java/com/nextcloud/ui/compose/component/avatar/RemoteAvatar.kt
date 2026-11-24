/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.GlideHelper
import com.owncloud.android.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val INITIALS_FONT_SIZE_RATIO = 0.45f
private const val INITIALS_BACKGROUND_ALPHA = 0.1f

@Composable
fun RemoteAvatar(client: NextcloudClient, path: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val avatar by produceState<ImageBitmap?>(null, client, path, sizePx) {
        value = withContext(Dispatchers.IO) {
            GlideHelper.getDrawable(context, client, client.baseUri.toString() + path, sizePx)
                ?.toBitmap(sizePx, sizePx)
                ?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        InitialsAvatar(name, size)

        avatar?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
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
