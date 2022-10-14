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

package com.owncloud.android.ui.preview

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.ActivityPreviewBitmapBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Zoomable preview of a single bitmap
 */
class PreviewBitmapActivity : AppCompatActivity(), Injectable {

    companion object {
        const val EXTRA_BITMAP_PATH = "EXTRA_BITMAP_PATH"
    }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: ActivityPreviewBitmapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBitmapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupImage()

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            viewThemeUtils.files.setWhiteBackButton(this, it)
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
