/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

    companion object {
        private const val SHOW_ZOOM_TIP_TIMES = 3
    }

    private var _pdfRenderer = MutableLiveData<PdfRenderer>()
    val pdfRenderer: LiveData<PdfRenderer>
        get() = _pdfRenderer

    private var _previewImagePath = MutableLiveData<String>()
    val previewImagePath: LiveData<String>
        get() = _previewImagePath

    private var _showZoomTip = MutableLiveData<Boolean>()
    val shouldShowZoomTip: LiveData<Boolean>
        get() = _showZoomTip

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

    fun process(file: OCFile) {
        closeRenderer()
        _pdfRenderer.value =
            PdfRenderer(ParcelFileDescriptor.open(File(file.storagePath), ParcelFileDescriptor.MODE_READ_ONLY))
        if (appPreferences.pdfZoomTipShownCount < SHOW_ZOOM_TIP_TIMES) {
            _showZoomTip.value = true
        }
    }

    fun onClickPage(page: Bitmap) {
        val file = File.createTempFile("pdf_page", ".bmp", MainApp.getAppContext().cacheDir)
        val outStream = FileOutputStream(file)
        page.compress(Bitmap.CompressFormat.PNG, 0, outStream)
        outStream.close()

        _previewImagePath.value = file.path
    }

    fun onZoomTipShown() {
        appPreferences.pdfZoomTipShownCount++
        _showZoomTip.value = false
    }
}
