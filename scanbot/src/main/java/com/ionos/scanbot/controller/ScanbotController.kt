/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.controller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.ionos.scanbot.upload.target_provider.UploadTarget
import com.ionos.scanbot.upload.target_provider.UploadTargetRepository
import io.reactivex.Observable

abstract class ScanbotController {
	abstract val fileUploadStarted: Observable<Any>
	abstract val uploadTargetRepository: UploadTargetRepository

	abstract fun setUpController(scanBotUploadTarget: UploadTarget)

	abstract fun createIntent(context: Context): Intent

	abstract fun scanToDocument(context: Context, path: String)

	internal abstract fun saveState(state: Bundle)

	internal abstract fun restoreState(state: Bundle)

	internal abstract fun onDocumentSaved(localUris: List<Uri>)
}
