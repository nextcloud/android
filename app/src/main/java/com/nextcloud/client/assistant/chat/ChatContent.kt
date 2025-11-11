/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.AssistantViewModel
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage

@Composable
fun ChatContent(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = false
    ) {
        items(chatMessages, key = { it.id }) { message ->
            ChatMessageItem(message = message)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val modifier = if (message.isHuman()) {
        Modifier
            .padding(start = 16.dp, end = 8.dp)
            .defaultMinSize(minHeight = 60.dp)
            .clip(RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp, bottomStart = 20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF007EF4),
                        Color(0xFF2A75BC),
                    )
                )
            )
    } else {
        Modifier
            .padding(start = 8.dp, end = 16.dp)
            .defaultMinSize(minHeight = 60.dp)
            .clip(RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp, bottomEnd = 20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF454545),
                        Color(0xFF2B2B2B),
                    )
                )
            )
    }

    val boxArrangement = if (message.isHuman()) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = boxArrangement) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!message.isHuman()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                {
                    Image(
                        painter = painterResource(id = R.drawable.ic_assistant),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }


            Box(
                modifier = modifier
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = message.content,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.timestampRepresentation(),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                        )
                    )
                }
            }
        }
    }
}
