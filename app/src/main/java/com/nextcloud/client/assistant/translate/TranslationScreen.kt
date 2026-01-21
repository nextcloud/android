/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.translate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nextcloud.client.assistant.AssistantViewModel
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationLanguage
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationLanguages
import com.owncloud.android.lib.resources.assistant.v2.model.toTranslationLanguages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(task: TaskTypeData?, viewModel: AssistantViewModel, textToTranslate: String) {
    var originText by remember { mutableStateOf(textToTranslate) }
    val languages = task?.toTranslationLanguages() ?: TranslationLanguages(listOf(), listOf())

    var originLanguage by remember { mutableStateOf(languages.originLanguages.first()) }
    var showOriginDropdownMenu by remember { mutableStateOf(false) }

    var targetText by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf(languages.targetLanguages.first()) }
    var showTargetDropdownMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 32.dp), floatingActionButton = {
            FloatingActionButton(onClick = {

            }, content = {
                Icon(
                    painter = painterResource(R.drawable.ic_translate),
                    contentDescription = "translate button"
                )
            })
        }) {
        LazyColumn(
            modifier = Modifier.padding(it)
        ) {
            item {
                LanguageSelector(
                    title = originLanguage,
                    languages = languages.originLanguages,
                    titleId = R.string.translation_screen_label_from,
                    expanded = showOriginDropdownMenu,
                    expand = {
                        showOriginDropdownMenu = it
                    }, onLanguageSelect = { newLanguage ->
                        originLanguage = newLanguage
                    }
                )

                TranslationTextField(
                    titleId = R.string.translation_screen_hint_source,
                    originText,
                    onValueChange = { updatedText ->
                        originText = updatedText
                    })
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            item {
                LanguageSelector(
                    title = targetLanguage,
                    languages = languages.targetLanguages,
                    titleId = R.string.translation_screen_label_to,
                    expanded = showTargetDropdownMenu,
                    expand = {
                        showTargetDropdownMenu = it
                    }, onLanguageSelect = { newLanguage ->
                        targetLanguage = newLanguage
                    }
                )

                TranslationTextField(
                    titleId = R.string.translation_screen_hint_target,
                    targetText,
                    onValueChange = { updatedText ->
                        targetText = updatedText
                    })
            }
        }
    }
}

@Composable
private fun TranslationTextField(titleId: Int, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = {
            onValueChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 240.dp),
        placeholder = {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.headlineSmall
            )
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

@Composable
private fun LanguageSelector(
    title: TranslationLanguage,
    languages: List<TranslationLanguage>,
    titleId: Int,
    expanded: Boolean,
    expand: (Boolean) -> Unit,
    onLanguageSelect: (TranslationLanguage) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = {
                expand(!expanded)
            })
    ) {
        Text(
            text = stringResource(titleId),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title.name,
            style = MaterialTheme.typography.labelLarge,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expand(false) }
        ) {
            languages.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.name) },
                    onClick = {
                        expand(false)
                        onLanguageSelect(language)
                    }
                )
            }
        }
    }
}
