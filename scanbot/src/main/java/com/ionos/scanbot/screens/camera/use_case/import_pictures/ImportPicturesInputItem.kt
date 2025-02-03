/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case.import_pictures

import android.net.Uri

internal sealed interface ImportPicturesInputItem {

	data class Picture(val uri: Uri) : ImportPicturesInputItem

	object CancellationSignal : ImportPicturesInputItem
}
