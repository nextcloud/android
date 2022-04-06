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

package com.owncloud.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.PermissionUtil
import com.zynksoftware.documentscanner.ScanActivity
import com.zynksoftware.documentscanner.model.DocumentScannerErrorModel
import com.zynksoftware.documentscanner.model.ScannerResults
import com.zynksoftware.documentscanner.ui.DocumentScanner

@Suppress("unused")
class AppScanActivity : ScanActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addFragmentContentLayout()
    }

    override fun onError(error: DocumentScannerErrorModel) {
        DisplayUtils.showSnackMessage(this, R.string.error_starting_scan_doc)
    }

    override fun onSuccess(scannerResults: ScannerResults) {
        val intent = Intent()

        intent.putExtra(
            "file",
            scannerResults.transformedImageFile?.absolutePath ?: scannerResults.croppedImageFile?.absolutePath
        )

        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onClose() {
        finish()
    }

    companion object {
        @JvmStatic
        val enabled: Boolean = true

        @JvmStatic
        fun scanFromCamera(activity: Activity, requestCode: Int) {
            val configuration = DocumentScanner.Configuration()
            configuration.imageType = Bitmap.CompressFormat.PNG
            DocumentScanner.init(activity, configuration)
            val scanIntent = Intent(activity, AppScanActivity::class.java)
            if (PermissionUtil.checkSelfPermission(activity, Manifest.permission.CAMERA)) {
                activity.startActivityForResult(scanIntent, requestCode)
            } else {
                PermissionUtil.requestCameraPermission(activity, PermissionUtil.PERMISSIONS_SCAN_DOCUMENT)
            }
        }
    }
}
