/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import androidx.core.util.Consumer
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFileName
import com.nextcloud.utils.fileNameValidator.FileNameValidator.isExtensionChanged
import com.nextcloud.utils.fileNameValidator.FileNameValidator.isFileHidden
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability

/**
 * A TextWatcher which wraps around [FileNameValidator]
 */
@Suppress("LongParameterList")
class FileNameTextWatcher(
    private val previousFileName: String?,
    private val context: Context,
    private val getCapabilities: () -> OCCapability,
    private val getExistingFileNames: () -> Set<String>?,
    private val onError: Consumer<String>,
    private val onWarning: Consumer<String>,
    private val onOkay: Runnable
) : TextWatcher {

    private var isOkay: Boolean = true // Used to trigger the onOkay callback only once

    override fun afterTextChanged(s: Editable?) {
        var newFileName = ""
        if (s != null) {
            newFileName = s.toString()
        }

        val errorMessage = checkFileName(newFileName, getCapabilities(), context, getExistingFileNames())

        if (isFileHidden(newFileName)) {
            isOkay = false
            onWarning.accept(context.getString(R.string.hidden_file_name_warning))
        } else if (errorMessage != null) {
            isOkay = false
            onError.accept(errorMessage)
        } else if (isExtensionChanged(previousFileName, newFileName)) {
            isOkay = false
            onWarning.accept(context.getString(R.string.warn_rename_extension))
        } else if (!isOkay) {
            isOkay = true
            onOkay.run()
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
}
