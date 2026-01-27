/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.assistant.repository.remote.AssistantRemoteRepository
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.assistant.v2.model.Task
import com.owncloud.android.lib.resources.assistant.v2.model.TaskTypeData
import com.owncloud.android.lib.resources.assistant.v2.model.TranslationRequest
import com.owncloud.android.lib.resources.assistant.v2.model.toTranslationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TranslationViewModel(private val remoteRepository: AssistantRemoteRepository) : ViewModel() {

    companion object {
        private const val TAG = "TranslationViewModel"
        private const val POLLING_INTERVAL_MS = 15_000L
        private const val MAX_RETRY = 3
    }

    private var _screenState =
        MutableStateFlow<TranslationScreenState>(Uninitialized)
    val screenState: StateFlow<TranslationScreenState>
        get() = _screenState

    private val _snackbarMessageId = MutableStateFlow<Int?>(null)
    val snackbarMessageId: StateFlow<Int?> = _snackbarMessageId

    private lateinit var taskTypeData: TaskTypeData
    private var task: Task? = null
    private var textToTranslate = ""
    private var translatedText = ""

    fun init(taskTypeData: TaskTypeData, task: Task?, textToTranslate: String) {
        this.task = task
        this.textToTranslate = textToTranslate
        this.taskTypeData = taskTypeData

        _screenState = if (task == null) {
            MutableStateFlow(NewTranslation.create(taskTypeData, textToTranslate))
        } else {
            val translatedText = task.output?.output ?: ""
            this.translatedText = translatedText
            MutableStateFlow(ExistingTranslation.create(taskTypeData, textToTranslate, translatedText))
        }
    }

    fun translate() {
        viewModelScope.launch(Dispatchers.IO) {
            val stateValue = _screenState.value
            val textToTranslate = stateValue.source.text
            val originLanguage = stateValue.source.language ?: return@launch
            val targetLanguage = stateValue.target.language ?: return@launch

            val model = taskTypeData.toTranslationModel()

            val input = TranslationRequest(
                input = textToTranslate,
                originLanguage = originLanguage.code,
                targetLanguage = targetLanguage.code,
                maxTokens = 0.0,
                model = model?.model ?: ""
            )

            val result = remoteRepository.translate(input, taskTypeData)
            if (result.isSuccess) {
                _screenState.update { it.withShimmer(true) }
                pollTranslationResult()
                _screenState.update { it.withShimmer(false) }
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun pollTranslationResult() {
        val screenStateValue = _screenState.value
        if (screenStateValue is Uninitialized) {
            return
        }

        val taskTypeId = taskTypeData.id ?: return
        val input = screenStateValue.source.text

        repeat(MAX_RETRY) { attempt ->
            val translationTasks = remoteRepository.getTaskList(taskTypeId)
            val translationResult = translationTasks
                ?.find { it.input?.input == input }
                ?.output
                ?.output

            if (!translationResult.isNullOrBlank()) {
                _screenState.update { it.withTargetText(translationResult) }
                return
            }

            Log_OC.d(TAG, "Translation not ready yet (attempt ${attempt + 1}/$MAX_RETRY)")

            if (attempt < MAX_RETRY - 1) {
                delay(POLLING_INTERVAL_MS)
            }
        }

        Log_OC.w(TAG, "Translation polling finished but result is still empty")
        updateSnackbarMessage(R.string.translation_screen_task_processing)
    }

    fun updateSnackbarMessage(value: Int?) {
        _snackbarMessageId.update {
            value
        }
    }

    fun updateSourceState(newSourceState: TranslationSideState) {
        _screenState.update { it.withSource(newSourceState) }
    }

    fun updateTargetState(newTargetState: TranslationSideState) {
        _screenState.update { it.withTarget(newTargetState) }
    }
}
