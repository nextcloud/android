/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:Suppress("TopLevelPropertyNaming", "MagicNumber")

package com.nextcloud.client.assistant.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.utils.TimeConstants
import com.nextcloud.utils.extensions.isHuman
import com.nextcloud.utils.extensions.time
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import java.time.Instant

private val MIN_CHAT_HEIGHT = 60.dp
private val CHAT_BUBBLE_CORNER_RADIUS = 8.dp
private val ASSISTANT_ICON_SIZE = 40.dp

@Composable
fun ChatContent(viewModel: AssistantViewModel, modifier: Modifier = Modifier) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAssistantAnswering by viewModel.isAssistantAnswering.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages) {
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = false,
        state = listState
    ) {
        items(chatMessages, key = { it.id }) { message ->
            if (message.isHuman()) {
                UserMessageItem(message)
            } else {
                AssistantMessageItem(message)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isAssistantAnswering) {
            item {
                AssistantTypingIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AssistantTypingIndicator() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedAssistantIcon()

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .defaultMinSize(minHeight = MIN_CHAT_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topEnd = CHAT_BUBBLE_CORNER_RADIUS,
                            topStart = CHAT_BUBBLE_CORNER_RADIUS,
                            bottomEnd = CHAT_BUBBLE_CORNER_RADIUS
                        )
                    )
                    .background(color = colorResource(R.color.task_container))
            ) {
                TypingAnimation()
            }
        }
    }
}

@Composable
private fun AnimatedAssistantIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "assistant_icon_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(TimeConstants.MILLIS_PER_SECOND, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    Box(
        modifier = Modifier
            .size(ASSISTANT_ICON_SIZE)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_assistant),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .scale(scale)
        )
    }
}

@Composable
private fun TypingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_animation")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_alpha"
    )

    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.assistant_thinking),
            style = TextStyle(
                color = colorResource(R.color.text_color).copy(alpha = alpha),
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun AssistantMessageItem(message: ChatMessage) {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .size(ASSISTANT_ICON_SIZE)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_assistant),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp)
                    .defaultMinSize(minHeight = MIN_CHAT_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topEnd = CHAT_BUBBLE_CORNER_RADIUS,
                            topStart = CHAT_BUBBLE_CORNER_RADIUS,
                            bottomEnd = CHAT_BUBBLE_CORNER_RADIUS
                        )
                    )
                    .background(
                        color = colorResource(R.color.task_container)
                    )
            ) {
                MessageTextItem(message)
            }
        }
    }
}

@Composable
private fun UserMessageItem(message: ChatMessage) {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .defaultMinSize(minHeight = MIN_CHAT_HEIGHT)
                    .clip(
                        RoundedCornerShape(
                            topEnd = CHAT_BUBBLE_CORNER_RADIUS,
                            topStart = CHAT_BUBBLE_CORNER_RADIUS,
                            bottomStart = CHAT_BUBBLE_CORNER_RADIUS
                        )
                    )
                    .background(color = colorResource(R.color.task_container))
            ) {
                MessageTextItem(message)
            }
        }
    }
}

@Composable
private fun MessageTextItem(message: ChatMessage) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = message.content,
            style = TextStyle(
                color = colorResource(R.color.text_color),
                fontSize = 16.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message.time(),
            style = TextStyle(
                color = colorResource(R.color.text_color),
                fontSize = 12.sp
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageTextItemPreview() {
    val mockMessages = listOf(
        ChatMessage(
            id = 1,
            sessionId = 101,
            role = "human",
            content = "Hey, how are you?",
            timestamp = Instant.now().epochSecond,
            ocpTaskId = null,
            sources = "",
            attachments = emptyList()
        ),
        ChatMessage(
            id = 2,
            sessionId = 101,
            role = "assistant",
            content = "I'm good! Here’s a message from yesterday.",
            timestamp = Instant.now().minusSeconds(86_400).epochSecond, // 1 day ago
            ocpTaskId = null,
            sources = "",
            attachments = emptyList()
        ),
        ChatMessage(
            id = 3,
            sessionId = 101,
            role = "human",
            content = "And an older one from last week.",
            timestamp = Instant.now().minusSeconds(7 * 86_400).epochSecond, // 7 days ago
            ocpTaskId = null,
            sources = "",
            attachments = emptyList()
        )
    )

    Column(modifier = Modifier.padding(16.dp)) {
        mockMessages.forEach { message ->
            MessageTextItem(message = message)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
