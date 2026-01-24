/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.translate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.AssistantViewModel
import com.nextcloud.client.assistant.model.AssistantScreenState
import com.nextcloud.utils.extensions.getActivity
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationLanguage
import com.owncloud.android.lib.resources.assistant.v2.model.toTranslationLanguages
import com.owncloud.android.utils.ClipboardUtil

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    selectedTaskType: TaskTypeData?,
    viewModel: AssistantViewModel,
    textToTranslate: String,
    isTaskExists: Boolean
) {
    val languages = remember(selectedTaskType) { selectedTaskType?.toTranslationLanguages() }
    val isTranslationTaskCreated by viewModel.isTranslationTaskCreated.collectAsState()
    val translationTaskOutput by viewModel.translationTaskOutput.collectAsState()

    var sourceState by remember {
        mutableStateOf(
            TranslationSideState(
                text = textToTranslate,
                language = languages?.originLanguages?.firstOrNull(),
                isTarget = false
            )
        )
    }
    var targetState by remember {
        mutableStateOf(TranslationSideState(language = languages?.targetLanguages?.firstOrNull(), isTarget = true))
    }

    LaunchedEffect(translationTaskOutput) {
        targetState = targetState.copy(text = translationTaskOutput)
    }

    BackHandler {
        viewModel.onTranslationScreenDismissed()
        viewModel.updateScreenState(AssistantScreenState.TaskContent)
    }

    // task is unselected
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onTranslationScreenDismissed()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp),
        floatingActionButton = {
            if (!isTaskExists) {
                // TODO: After first task creation dont allow user to create another back to back
                FloatingActionButton(onClick = {
                    val originLang = sourceState.language
                    val targetLang = targetState.language
                    if (originLang != null && targetLang != null) {
                        viewModel.translate(sourceState.text, originLang, targetLang)
                    }
                }, content = {
                    Icon(painter = painterResource(R.drawable.ic_translate), contentDescription = "translate button")
                })
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                TranslationSection(
                    labelId = R.string.translation_screen_label_from,
                    hintId = R.string.translation_screen_hint_source,
                    state = sourceState,
                    availableLanguages = languages?.originLanguages ?: emptyList(),
                    maxDp = 120.dp,
                    shimmer = false,
                    onStateChange = { sourceState = it }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            item {
                TranslationSection(
                    labelId = R.string.translation_screen_label_to,
                    hintId = if (isTaskExists) {
                        R.string.translation_screen_translating
                    } else {
                        R.string.translation_screen_start_to_translate_task
                    },
                    state = targetState,
                    availableLanguages = languages?.targetLanguages ?: emptyList(),
                    maxDp = Dp.Unspecified,
                    shimmer = isTaskExists || isTranslationTaskCreated,
                    onStateChange = { targetState = it }
                )
            }
        }
    }
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun TranslationSection(
    labelId: Int,
    hintId: Int,
    state: TranslationSideState,
    availableLanguages: List<TranslationLanguage>,
    maxDp: Dp,
    shimmer: Boolean,
    onStateChange: (TranslationSideState) -> Unit
) {
    val activity = LocalContext.current.getActivity()

    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onStateChange(state.copy(isExpanded = !state.isExpanded)) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = state.language?.name ?: "",
            style = MaterialTheme.typography.labelLarge
        )
        Icon(
            painter = painterResource(R.drawable.ic_baseline_arrow_drop_down_24),
            contentDescription = "dropdown icon",
            modifier = Modifier
                .padding(start = 4.dp)
                .size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        DropdownMenu(
            expanded = state.isExpanded,
            onDismissRequest = { onStateChange(state.copy(isExpanded = false)) }
        ) {
            availableLanguages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.name) },
                    onClick = {
                        onStateChange(state.copy(language = language, isExpanded = false))
                    }
                )
            }
        }

        if (state.isTarget && state.text.isNotBlank()) {
            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {
                activity?.let { ClipboardUtil.copyToClipboard(it, state.text, true) }
            }) {
                Icon(painter = painterResource(R.drawable.ic_content_copy), contentDescription = "copy button")
            }
        }
    }

    if (state.isTarget && shimmer) {
        TranslatingShimmer(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(horizontal = 16.dp)
        )
    } else {
        TextField(
            value = state.text,
            onValueChange = { onStateChange(state.copy(text = it)) },
            readOnly = state.isTarget,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = maxDp),
            placeholder = {
                Text(text = stringResource(hintId), style = MaterialTheme.typography.headlineSmall)
            },
            textStyle = MaterialTheme.typography.headlineSmall,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun TranslatingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.translation_screen_translating),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}
