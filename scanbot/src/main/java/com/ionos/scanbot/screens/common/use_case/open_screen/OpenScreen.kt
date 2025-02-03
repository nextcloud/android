/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.common.use_case.open_screen

import android.app.Activity
import android.content.Intent
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.*
import com.ionos.scanbot.screens.camera.CameraActivity
import com.ionos.scanbot.screens.crop.CropActivity
import com.ionos.scanbot.screens.filter.FilterActivity
import com.ionos.scanbot.screens.gallery.GalleryActivity
import com.ionos.scanbot.screens.rearrange.RearrangeActivity
import com.ionos.scanbot.screens.save.SaveActivity

internal class OpenScreen(private val activity: Activity) {

	operator fun invoke(intent: OpenScreenIntent) {
        activity.startActivityForResult(intent.toAndroidIntent(), intent.requestCode)
		if (intent.closeCurrent) {
			activity.finish()
		}
	}

	private fun OpenScreenIntent.toAndroidIntent(): Intent = when (this) {
		is OpenCameraScreenIntent -> Intent(activity, CameraActivity::class.java)
		is OpenGalleryScreenIntent -> GalleryActivity.createIntent(activity, pictureId)
		is OpenCropScreenIntent -> CropActivity.createIntent(activity, pictureId)
		is OpenFilterScreenIntent -> FilterActivity.createIntent(activity, pictureId, filterType)
		is OpenRearrangeScreenIntent -> Intent(activity, RearrangeActivity::class.java)
		is OpenSaveScreenIntent -> Intent(activity, SaveActivity::class.java)
	}
}
