/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PreviewPdfViewModel @Inject constructor(val appPreferences: AppPreferences) : ViewModel() {

    private var _pdfRenderer = MutableLiveData<PdfRenderer>()
    val pdfRenderer: LiveData<PdfRenderer>
        get() = _pdfRenderer

    private var _previewImagePath = MutableLiveData<String>()
    val previewImagePath: LiveData<String>
        get() = _previewImagePath

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }

    private fun closeRenderer() {
        try {
            _pdfRenderer.value?.close()
        } catch (e: IllegalStateException) {
            Log_OC.e(this, "closeRenderer: trying to close already closed renderer", e)
        }
    }

    /**
     * @throws SecurityException if file points to a password-protected document
     */
    fun process(file: OCFile) {
        closeRenderer()
        _pdfRenderer.value =
            PdfRenderer(ParcelFileDescriptor.open(File(file.storagePath), ParcelFileDescriptor.MODE_READ_ONLY))
    }

    fun onClickPage(page: Bitmap) {
        val file = File.createTempFile("pdf_page", ".bmp", MainApp.getAppContext().cacheDir)
        val outStream = FileOutputStream(file)
        page.compress(Bitmap.CompressFormat.PNG, 0, outStream)
        outStream.close()

        _previewImagePath.value = file.path
    }
}
