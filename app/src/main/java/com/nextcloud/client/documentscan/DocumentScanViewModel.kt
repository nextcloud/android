/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.documentscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.IoDispatcher
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.logger.Logger
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.helpers.FileOperationsHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@Suppress("Detekt.LongParameterList") // satisfied by DI
class DocumentScanViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    app: Application,
    private val logger: Logger,
    private val backgroundJobManager: BackgroundJobManager,
    private val currentAccountProvider: CurrentAccountProvider
) : AndroidViewModel(app) {
    init {
        logger.d(TAG, "DocumentScanViewModel created")
    }

    sealed interface UIState {
        sealed class BaseState(val pageList: List<String>) : UIState {
            val isEmpty: Boolean
                get() = pageList.isEmpty()
        }

        class NormalState(pageList: List<String> = emptyList(), val shouldRequestScan: Boolean = false) :
            BaseState(pageList)

        class RequestExportState(pageList: List<String> = emptyList(), val shouldRequestExportType: Boolean = true) :
            BaseState(pageList)

        object DoneState : UIState
        object CanceledState : UIState
    }

    private var uploadFolder: String? = null
    private val initialState = UIState.NormalState(shouldRequestScan = true)
    private val _uiState = MutableLiveData<UIState>(initialState)
    val uiState: LiveData<UIState>
        get() = _uiState

    /**
     * @param result should be the path to the scanned page on the disk
     */
    fun onScanPageResult(result: String?) {
        logger.d(TAG, "onScanPageResult() called with: result = $result")

        val state = _uiState.value
        require(state is UIState.NormalState)

        viewModelScope.launch(ioDispatcher) {
            if (result != null) {
                val newPath = renameCapturedImage(result)
                val pageList = state.pageList.toMutableList()
                pageList.add(newPath)
                _uiState.postValue(UIState.NormalState(pageList))
            } else {
                // result == null means cancellation or error
                if (state.isEmpty) {
                    // close only if no pages have been added yet
                    _uiState.postValue(UIState.CanceledState)
                }
            }
        }
    }

    // TODO extract to usecase
    private fun renameCapturedImage(originalPath: String): String {
        val file = File(originalPath)
        val renamedFile =
            File(
                getApplication<Application>().cacheDir.path +
                    File.separator + FileOperationsHelper.getCapturedImageName()
            )
        file.renameTo(renamedFile)
        return renamedFile.absolutePath
    }

    fun onScanRequestHandled() {
        val state = uiState.value
        require(state is UIState.NormalState)

        _uiState.postValue(UIState.NormalState(state.pageList, shouldRequestScan = false))
    }

    fun onAddPageClicked() {
        val state = uiState.value
        require(state is UIState.NormalState)
        if (!state.shouldRequestScan) {
            _uiState.postValue(UIState.NormalState(state.pageList, shouldRequestScan = true))
        }
    }

    fun onClickDone() {
        val state = _uiState.value
        if (state is UIState.BaseState && !state.isEmpty) {
            _uiState.postValue(UIState.RequestExportState(state.pageList))
        }
    }

    fun setUploadFolder(folder: String) {
        this.uploadFolder = folder
    }

    fun onRequestTypeHandled() {
        val state = _uiState.value
        require(state is UIState.RequestExportState)
        _uiState.postValue(UIState.RequestExportState(state.pageList, false))
    }

    fun onExportTypeSelected(exportType: ExportType) {
        val state = _uiState.value
        require(state is UIState.RequestExportState)
        when (exportType) {
            ExportType.PDF -> {
                exportToPdf(state.pageList)
            }
            ExportType.IMAGES -> {
                exportToImages(state.pageList)
            }
        }
        _uiState.postValue(UIState.DoneState)
    }

    private fun exportToPdf(pageList: List<String>) {
        val genPath =
            getApplication<Application>().cacheDir.path + File.separator + FileOperationsHelper.getTimestampedFileName(
                ".pdf"
            )
        backgroundJobManager.startPdfGenerateAndUploadWork(
            currentAccountProvider.user,
            uploadFolder!!,
            pageList,
            genPath
        )
        // after job is started, finish the application.
        _uiState.postValue(UIState.DoneState)
    }

    private fun exportToImages(pageList: List<String>) {
        val uploadPaths = pageList.map {
            uploadFolder + OCFile.PATH_SEPARATOR + File(it).name
        }.toTypedArray()

        FileUploadHelper.instance().uploadNewFiles(
            currentAccountProvider.user,
            pageList.toTypedArray(),
            uploadPaths,
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.ASK_USER
        )
    }

    fun onExportCanceled() {
        val state = _uiState.value
        if (state is UIState.BaseState) {
            _uiState.postValue(UIState.NormalState(state.pageList))
        }
    }

    private companion object {
        private const val TAG = "DocumentScanViewModel"
    }

    enum class ExportType {
        PDF,
        IMAGES
    }
}
