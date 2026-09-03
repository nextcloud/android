/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import com.elyeproj.loaderviewlibrary.LoaderImageView

data class ThumbnailArguments(val isGrid: Boolean, val hideVideoOverlay: Boolean, val shimmer: LoaderImageView?) {
    companion object {
        val none = ThumbnailArguments(isGrid = false, hideVideoOverlay = false, shimmer = null)

        fun withShimmer(view: LoaderImageView): ThumbnailArguments = none.copy(shimmer = view)
    }
}
