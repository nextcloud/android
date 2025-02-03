/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.upload.use_case

import android.net.Uri
import javax.inject.Inject

internal class StartUpload @Inject constructor(
	private val uploader: Uploader,
){
	operator fun invoke(localUris: List<Uri>, remotePath: String) {
        uploader.upload(
            remotePath,
            localUris.mapNotNull { uri -> uri.path })
	}
}
