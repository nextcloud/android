/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R

class StandardDirectory private constructor(
    val name: String,
    @StringRes val displayName: Int,
    @DrawableRes val icon: Int
) {
    companion object {
        @JvmField
        val PICTURES = StandardDirectory(
            Environment.DIRECTORY_PICTURES,
            R.string.storage_pictures,
            R.drawable.ic_image_grey600
        )

        @JvmField
        val CAMERA = StandardDirectory(
            Environment.DIRECTORY_DCIM,
            R.string.storage_camera,
            R.drawable.ic_camera
        )

        @JvmField
        val DOCUMENTS = StandardDirectory(
            Environment.DIRECTORY_DOCUMENTS,
            R.string.storage_documents,
            R.drawable.ic_document_grey600
        )

        @JvmField
        val DOWNLOADS = StandardDirectory(
            Environment.DIRECTORY_DOWNLOADS,
            R.string.storage_downloads,
            R.drawable.ic_download_grey600
        )

        @JvmField
        val MOVIES = StandardDirectory(
            Environment.DIRECTORY_MOVIES,
            R.string.storage_movies,
            R.drawable.ic_movie_grey600
        )

        @JvmField
        val MUSIC = StandardDirectory(
            Environment.DIRECTORY_MUSIC,
            R.string.storage_music,
            R.drawable.ic_music_grey600
        )

        private val ALL: Set<StandardDirectory> = hashSetOf(PICTURES, CAMERA, DOCUMENTS, DOWNLOADS, MOVIES, MUSIC)

        @JvmStatic
        fun getStandardDirectories(): Collection<StandardDirectory> = ALL

        @JvmStatic
        fun fromPath(path: String?): StandardDirectory? = ALL.firstOrNull { it.name == path }
    }
}
