/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
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
        val bitmap = createBitmap(screenWidth, (screenWidth.toFloat() / page.width * page.height).toInt())

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return bitmap
    }
}
