/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail.job

import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.thumbnail.ResizedImageGenerator
import com.nextcloud.utils.thumbnail.VideoOverlayGenerator
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.preview.media.PreviewMediaFragment
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ResizedImageGenerationJob(
    private val fragment: PreviewMediaFragment,
    private val storageManager: FileDataStorageManager,
    private val connectivityService: ConnectivityService,
    private val clientRepository: ClientRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "ResizedImageGenerationJob"
    }

    suspend fun load(file: OCFile, imageView: ImageView, progressContainer: FrameLayout) {
        val image = getOrCreateDisplayImage(file)
        if (image == null) {
            Log_OC.e(TAG, "load failed, no resized image: ${file.remotePath}")
            showLoadError()
            return
        }

        if (imageView.tag?.toString() != file.fileId.toString()) {
            Log_OC.d(TAG, "load skipped, image view now shows another file: ${file.remotePath}")
            return
        }

        imageView.run {
            visibility = View.VISIBLE
            setImageBitmap(image)
            setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_inverse))
        }
        progressContainer.visibility = View.GONE
        Log_OC.d(TAG, "load completed: ${file.remotePath}")
    }

    private suspend fun getOrCreateDisplayImage(file: OCFile): Bitmap? {
        val client = clientRepository.getOwncloudClient()
        if (client == null) {
            Log_OC.e(TAG, "no client available: ${file.remotePath}")
            return null
        }
        ThumbnailsCacheManager.setClient(client)
        return withContext(ioDispatcher) { getOrCreateWithVideoOverlay(file, client) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getOrCreateWithVideoOverlay(file: OCFile, client: OwnCloudClient): Bitmap? = try {
        val resizedImage = ResizedImageGenerator.getOrCreate(file, client, storageManager)
        if (resizedImage != null && MimeTypeUtil.isVideo(file)) {
            VideoOverlayGenerator.addOverlay(resizedImage, MainApp.getAppContext())
        } else {
            resizedImage
        }
    } catch (t: Throwable) {
        Log_OC.e(TAG, "resized image generation crashed: ${file.remotePath}", t)
        null
    }

    private suspend fun showLoadError() {
        val isInternetWalled = withContext(ioDispatcher) { connectivityService.isInternetWalled() }
        if (isInternetWalled) {
            Log_OC.d(TAG, "showing no connection error")
            fragment.setNoConnectionErrorMessage()
        } else {
            Log_OC.d(TAG, "showing unsupported image error")
            fragment.handleUnsupportedImage()
        }
    }
}
