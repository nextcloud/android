/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import com.zynksoftware.documentscanner.ScanActivity
import com.zynksoftware.documentscanner.model.DocumentScannerErrorModel
import com.zynksoftware.documentscanner.model.ScannerResults

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
}
