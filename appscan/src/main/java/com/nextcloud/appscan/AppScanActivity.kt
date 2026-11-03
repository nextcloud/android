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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.children
import com.nextcloud.android.common.ui.util.extensions.applyEdgeToEdgeWithSystemBarPadding
import com.zynksoftware.documentscanner.R
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreateCropFragmentView()
    }

    private fun recreateCropFragmentView() {
        val cropFragment = supportFragmentManager.findFragmentByTag(IMAGE_CROP_FRAGMENT_TAG) ?: return
        if (!cropFragment.isVisible) {
            return
        }

        val cornerFractions = cropFragment.view?.let(::cropCornerFractions)

        supportFragmentManager.beginTransaction().detach(cropFragment).commitNow()
        supportFragmentManager.beginTransaction().attach(cropFragment).commitNow()

        val cropView = cropFragment.view ?: return
        if (cornerFractions == null) {
            return
        }

        cropView.post { restoreCropCorners(cropView, cornerFractions) }
    }

    private fun cropCornerFractions(cropView: View): List<PointF>? {
        val preview = cropPreviewBitmap(cropView) ?: return null
        return cropCornerHandles(cropView).map { PointF(it.x / preview.width, it.y / preview.height) }
    }

    private fun restoreCropCorners(cropView: View, cornerFractions: List<PointF>) {
        val preview = cropPreviewBitmap(cropView) ?: return
        cropCornerHandles(cropView).zip(cornerFractions) { handle, fraction ->
            handle.x = fraction.x * preview.width
            handle.y = fraction.y * preview.height
        }
        cropView.findViewById<View>(R.id.polygonView).invalidate()
    }

    private fun cropPreviewBitmap(cropView: View): Bitmap? =
        (cropView.findViewById<ImageView>(R.id.imagePreview).drawable as? BitmapDrawable)?.bitmap

    private fun cropCornerHandles(cropView: View): List<View> =
        cropView.findViewById<ViewGroup>(R.id.polygonView).children.toList()

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

        private const val IMAGE_CROP_FRAGMENT_TAG = "ImageCropFragmentTag"
    }
}
