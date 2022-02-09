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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.databinding.PreviewPdfPageItemBinding

/**
 * @param renderer an **open** [PdfRenderer]
 */
class PreviewPdfAdapter(
    private val renderer: PdfRenderer,
    private val screenWidth: Int,
    private val onClickListener: (Bitmap) -> Unit
) :
    RecyclerView.Adapter<PreviewPdfAdapter.ViewHolder>() {

    class ViewHolder(val binding: PreviewPdfPageItemBinding, val onClickListener: (Bitmap) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(bitmap: Bitmap) {
            binding.page.setImageBitmap(bitmap)
            binding.root.setOnClickListener {
                onClickListener(bitmap)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PreviewPdfPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClickListener)
    }

    override fun getItemCount() = renderer.pageCount

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = renderer.openPage(position)
        val bitmap = renderPage(page)
        holder.bind(bitmap)
    }

    private fun renderPage(page: PdfRenderer.Page) = page.use {
        val bitmap = createBitmapForPage(it)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    }

    private fun createBitmapForPage(page: PdfRenderer.Page): Bitmap {
        val bitmap = Bitmap.createBitmap(
            screenWidth, (screenWidth.toFloat() / page.width * page.height).toInt(), Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return bitmap
    }
}
