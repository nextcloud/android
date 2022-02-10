/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
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
        if (!appPreferences.isPdfZoomTipShown) {
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
        appPreferences.isPdfZoomTipShown = true
        _showZoomTip.value = false
    }
}
