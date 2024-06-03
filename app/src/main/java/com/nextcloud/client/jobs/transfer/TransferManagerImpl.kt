/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.transfer

import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.IsCancelled
import com.nextcloud.client.core.OnProgressCallback
import com.nextcloud.client.core.TaskFunction
import com.nextcloud.client.files.DownloadRequest
import com.nextcloud.client.files.Registry
import com.nextcloud.client.files.Request
import com.nextcloud.client.files.UploadRequest
import com.nextcloud.client.jobs.download.DownloadTask
import com.nextcloud.client.jobs.upload.UploadTask
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.UploadFileOperation
import java.util.UUID

/**
 * Per-user file transfer manager.
 *
 * All notifications are performed on main thread. All transfer processes are run
 * in the background.
 *
 * @param runner Background task runner. It is important to provide runner that is not shared with UI code.
 * @param downloadTaskFactory Download task factory
 * @param threads maximum number of concurrent transfer processes
 */
@Suppress("LongParameterList") // transfer operations requires those resources
class TransferManagerImpl(
    private val runner: AsyncRunner,
    private val downloadTaskFactory: DownloadTask.Factory,
    private val uploadTaskFactory: UploadTask.Factory,
    threads: Int = 1
) : TransferManager {

    companion object {
        const val PROGRESS_PERCENTAGE_MAX = 100
        const val PROGRESS_PERCENTAGE_MIN = 0
        const val TEST_DOWNLOAD_PROGRESS_UPDATE_PERIOD_MS = 200L
        const val TEST_UPLOAD_PROGRESS_UPDATE_PERIOD_MS = 200L
    }

    private val registry = Registry(
        onStartTransfer = this::onStartTransfer,
        onTransferChanged = this::onTransferUpdate,
        maxRunning = threads
    )
    private val transferListeners: MutableSet<(Transfer) -> Unit> = mutableSetOf()
    private val statusListeners: MutableSet<(TransferManager.Status) -> Unit> = mutableSetOf()

    override val isRunning: Boolean get() = registry.isRunning

    override val status: TransferManager.Status
        get() = TransferManager.Status(
            pending = registry.pending,
            running = registry.running,
            completed = registry.completed
        )

    override fun registerTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.add(listener)
    }

    override fun removeTransferListener(listener: (Transfer) -> Unit) {
        transferListeners.remove(listener)
    }

    override fun registerStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.add(listener)
    }

    override fun removeStatusListener(listener: (TransferManager.Status) -> Unit) {
        statusListeners.remove(listener)
    }

    override fun enqueue(request: Request) {
        registry.add(request)
        registry.startNext()
    }

    override fun getTransfer(uuid: UUID): Transfer? = registry.getTransfer(uuid)

    override fun getTransfer(file: OCFile): Transfer? = registry.getTransfer(file)

    private fun onStartTransfer(uuid: UUID, request: Request) {
        if (request is DownloadRequest) {
            runner.postTask(
                task = createDownloadTask(request),
                onProgress = { progress: Int -> registry.progress(uuid, progress) },
                onResult = { result ->
                    registry.complete(uuid, result.success, result.file)
                    registry.startNext()
                },
                onError = {
                    registry.complete(uuid, false)
                    registry.startNext()
                }
            )
        } else if (request is UploadRequest) {
            runner.postTask(
                task = createUploadTask(request),
                onProgress = { progress: Int -> registry.progress(uuid, progress) },
                onResult = { result ->
                    registry.complete(uuid, result.success, result.file)
                    registry.startNext()
                },
                onError = {
                    registry.complete(uuid, false)
                    registry.startNext()
                }
            )
        }
    }

    private fun createDownloadTask(request: DownloadRequest): TaskFunction<DownloadTask.Result, Int> {
        return if (request.test) {
            { progress: OnProgressCallback<Int>, isCancelled: IsCancelled ->
                testDownloadTask(request.file, progress, isCancelled)
            }
        } else {
            val downloadTask = downloadTaskFactory.create()
            val wrapper: TaskFunction<DownloadTask.Result, Int> = { progress: ((Int) -> Unit), isCancelled ->
                downloadTask.download(request, progress, isCancelled)
            }
            wrapper
        }
    }

    private fun createUploadTask(request: UploadRequest): TaskFunction<UploadTask.Result, Int> {
        return if (request.test) {
            { progress: OnProgressCallback<Int>, isCancelled: IsCancelled ->
                val file = UploadFileOperation.obtainNewOCFileToUpload(
                    request.upload.remotePath,
                    request.upload.localPath,
                    request.upload.mimeType
                )
                testUploadTask(file, progress, isCancelled)
            }
        } else {
            val uploadTask = uploadTaskFactory.create()
            val wrapper: TaskFunction<UploadTask.Result, Int> = { _: ((Int) -> Unit), _ ->
                uploadTask.upload(request.user, request.upload)
            }
            wrapper
        }
    }

    private fun onTransferUpdate(transfer: Transfer) {
        transferListeners.forEach { it.invoke(transfer) }
        if (statusListeners.isNotEmpty()) {
            val status = this.status
            statusListeners.forEach { it.invoke(status) }
        }
    }

    /**
     *  Test download task is used only to simulate download process without
     *  any network traffic. It is used for development.
     */
    private fun testDownloadTask(
        file: OCFile,
        onProgress: OnProgressCallback<Int>,
        isCancelled: IsCancelled
    ): DownloadTask.Result {
        for (i in PROGRESS_PERCENTAGE_MIN..PROGRESS_PERCENTAGE_MAX) {
            onProgress(i)
            if (isCancelled()) {
                return DownloadTask.Result(file, false)
            }
            Thread.sleep(TEST_DOWNLOAD_PROGRESS_UPDATE_PERIOD_MS)
        }
        return DownloadTask.Result(file, true)
    }

    /**
     *  Test upload task is used only to simulate upload process without
     *  any network traffic. It is used for development.
     */
    private fun testUploadTask(
        file: OCFile,
        onProgress: OnProgressCallback<Int>,
        isCancelled: IsCancelled
    ): UploadTask.Result {
        for (i in PROGRESS_PERCENTAGE_MIN..PROGRESS_PERCENTAGE_MAX) {
            onProgress(i)
            if (isCancelled()) {
                return UploadTask.Result(file, false)
            }
            Thread.sleep(TEST_UPLOAD_PROGRESS_UPDATE_PERIOD_MS)
        }
        return UploadTask.Result(file, true)
    }
}
