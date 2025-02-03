/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.common.use_case.open_screen

import com.ionos.scanbot.filter.color.ColorFilterType

private const val UNDEFINED_REQUEST_CODE = -1

internal sealed interface OpenScreenIntent {
    val closeCurrent: Boolean
    val requestCode: Int

    data class OpenCameraScreenIntent(
        override val closeCurrent: Boolean,
        override val requestCode: Int = UNDEFINED_REQUEST_CODE,
    ) : OpenScreenIntent

    data class OpenGalleryScreenIntent(
        val pictureId: String,
        override val closeCurrent: Boolean,
        override val requestCode: Int = UNDEFINED_REQUEST_CODE,
    ) : OpenScreenIntent

    data class OpenCropScreenIntent(
        val pictureId: String,
        override val closeCurrent: Boolean,
        override val requestCode: Int = UNDEFINED_REQUEST_CODE,
    ) : OpenScreenIntent

    data class OpenFilterScreenIntent(
        val pictureId: String,
        val filterType: ColorFilterType,
        override val closeCurrent: Boolean,
        override val requestCode: Int = UNDEFINED_REQUEST_CODE,
    ) : OpenScreenIntent

    data class OpenRearrangeScreenIntent(
        override val closeCurrent: Boolean,
        override val requestCode: Int = UNDEFINED_REQUEST_CODE,
    ) : OpenScreenIntent

    data class OpenSaveScreenIntent(
        override val closeCurrent: Boolean,
        override val requestCode: Int = REQUEST_CODE,
    ) : OpenScreenIntent {

        companion object {
            const val REQUEST_CODE = 55006
        }
    }
}
