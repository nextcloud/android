/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.translate

import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.assistant.v2.model.toTranslationLanguages

@Suppress("LongParameterList")
sealed class TranslationScreenState(
    open val taskTypeData: TaskTypeData,
    open val source: TranslationSideState,
    open val target: TranslationSideState,
    open val fabVisibility: Boolean,
    open val shimmer: Boolean,
    open val targetHintMessageId: Int?
)

data class NewTranslation(
    override val taskTypeData: TaskTypeData,
    override val source: TranslationSideState,
    override val target: TranslationSideState,
    override val shimmer: Boolean = false
) : TranslationScreenState(
    taskTypeData = taskTypeData,
    source = source,
    target = target,
    fabVisibility = true,
    shimmer = shimmer,
    targetHintMessageId = R.string.translation_screen_start_to_translate_task
) {
    companion object {
        fun create(taskTypeData: TaskTypeData, textToTranslate: String): NewTranslation = NewTranslation(
            taskTypeData = taskTypeData,
            source = TranslationSideState(
                text = textToTranslate,
                language = taskTypeData.toTranslationLanguages().originLanguages.firstOrNull(),
                isTarget = false
            ),
            target = TranslationSideState(
                text = "",
                language = taskTypeData.toTranslationLanguages().targetLanguages.firstOrNull(),
                isTarget = true
            )
        )
    }
}

data class ExistingTranslation(
    override val taskTypeData: TaskTypeData,
    override val source: TranslationSideState,
    override val target: TranslationSideState,
    override val shimmer: Boolean = false
) : TranslationScreenState(
    taskTypeData = taskTypeData,
    source = source,
    target = target,
    fabVisibility = false,
    shimmer = shimmer,
    targetHintMessageId = null
) {
    companion object {
        fun create(taskTypeData: TaskTypeData, textToTranslate: String, translatedText: String): ExistingTranslation =
            ExistingTranslation(
                taskTypeData = taskTypeData,
                source = TranslationSideState(
                    text = textToTranslate,
                    language = taskTypeData.toTranslationLanguages().originLanguages.firstOrNull(),
                    isTarget = false
                ),
                target = TranslationSideState(
                    text = translatedText,
                    language = taskTypeData.toTranslationLanguages().targetLanguages.firstOrNull(),
                    isTarget = true
                )
            )
    }
}

data class EditedTranslation(
    override val taskTypeData: TaskTypeData,
    override val source: TranslationSideState,
    override val target: TranslationSideState,
    override val shimmer: Boolean = false
) : TranslationScreenState(
    taskTypeData = taskTypeData,
    source = source,
    target = target,
    fabVisibility = true,
    shimmer = shimmer,
    targetHintMessageId = null
) {
    companion object {
        fun create(taskTypeData: TaskTypeData, textToTranslate: String, translatedText: String): EditedTranslation =
            EditedTranslation(
                taskTypeData = taskTypeData,
                source = TranslationSideState(
                    text = textToTranslate,
                    language = taskTypeData.toTranslationLanguages().originLanguages.firstOrNull(),
                    isTarget = false
                ),
                target = TranslationSideState(
                    text = translatedText,
                    language = taskTypeData.toTranslationLanguages().targetLanguages.firstOrNull(),
                    isTarget = true
                )
            )
    }
}

fun TranslationScreenState.withShimmer(shimmer: Boolean): TranslationScreenState = when (this) {
    is NewTranslation -> copy(shimmer = shimmer)
    is ExistingTranslation -> copy(shimmer = shimmer)
    is EditedTranslation -> copy(shimmer = shimmer)
}

fun TranslationScreenState.withTargetText(text: String): TranslationScreenState = when (this) {
    is NewTranslation -> EditedTranslation(
        taskTypeData = taskTypeData,
        source = source,
        target = target.copy(text = text),
        shimmer = shimmer
    )
    is ExistingTranslation -> copy(
        target = target.copy(text = text)
    )
    is EditedTranslation -> copy(
        target = target.copy(text = text)
    )
}

fun TranslationScreenState.withSource(newSource: TranslationSideState): TranslationScreenState = when (this) {
    is NewTranslation -> copy(source = newSource)
    is ExistingTranslation -> EditedTranslation(
        taskTypeData = taskTypeData,
        source = newSource,
        target = target,
        shimmer = shimmer
    )
    is EditedTranslation -> copy(source = newSource)
}

fun TranslationScreenState.withTarget(newTarget: TranslationSideState): TranslationScreenState = when (this) {
    is NewTranslation -> {
        copy(target = newTarget)
    }
    is ExistingTranslation -> EditedTranslation(
        taskTypeData = taskTypeData,
        source = source,
        target = newTarget,
        shimmer = shimmer
    )
    is EditedTranslation -> copy(target = newTarget)
}
