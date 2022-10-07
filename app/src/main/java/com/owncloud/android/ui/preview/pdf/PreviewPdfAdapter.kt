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
            screenWidth,
            (screenWidth.toFloat() / page.width * page.height).toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return bitmap
    }
}
