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

package com.owncloud.android.ui.preview

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.databinding.ActivityPreviewBitmapBinding

/**
 * Zoomable preview of a single bitmap
 */
class PreviewBitmapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BITMAP_PATH = "EXTRA_BITMAP_PATH"
    }

    private lateinit var binding: ActivityPreviewBitmapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBitmapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImage()

        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupImage() {
        val path = intent.getStringExtra(EXTRA_BITMAP_PATH)
        require(path != null)

        val bitmap = BitmapFactory.decodeFile(path)
        binding.image.setImageBitmap(bitmap)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
