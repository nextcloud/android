/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.appscan

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.nextcloud.android.common.ui.util.extensions.applyEdgeToEdgeWithSystemBarPadding
import com.zynksoftware.documentscanner.ScanActivity
import com.zynksoftware.documentscanner.model.DocumentScannerErrorModel
import com.zynksoftware.documentscanner.model.ScannerResults
import com.zynksoftware.documentscanner.ui.DocumentScanner

@Suppress("unused")
class AppScanActivity : ScanActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeWithSystemBarPadding()
        super.onCreate(null)
        DocumentScanner.init(this)
        addFragmentContentLayout()
    }

    override fun onError(error: DocumentScannerErrorModel) = Unit

    override fun onSuccess(scannerResults: ScannerResults) {
        val intent = Intent()

        intent.putExtra(
            EXTRA_FILE,
            scannerResults.transformedImageFile?.absolutePath ?: scannerResults.croppedImageFile?.absolutePath
        )

        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onClose() {
        if (isChangingConfigurations) {
            return
        }

        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.contains(android.Manifest.permission.CAMERA) &&
            (grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED)
        ) {
            onClose()
        }
    }

    companion object {
        @JvmStatic
        val enabled: Boolean = true

        const val EXTRA_FILE = "file"
    }
}
