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
    private val capabilitiesProvider: () -> OCCapability,
    private val existingFileNamesProvider: () -> Set<String>?,
    private val onValidationError: Consumer<String>,
    private val onValidationWarning: Consumer<String>,
    private val onValidationSuccess: Runnable
) : TextWatcher {

    // Used to trigger the onValidationSuccess callback only once (on "error/warn -> valid" transition)
    private var isNameCurrentlyValid: Boolean = true

    override fun afterTextChanged(s: Editable?) {
        val currentFileName = s?.toString().orEmpty()
        val validationError = checkFileName(
            currentFileName,
            capabilitiesProvider(),
            context,
            existingFileNamesProvider()
        )

        when {
            isFileHidden(currentFileName) -> {
                isNameCurrentlyValid = false
                onValidationWarning.accept(context.getString(R.string.hidden_file_name_warning))
            }

            validationError != null -> {
                isNameCurrentlyValid = false
                onValidationError.accept(validationError)
            }

            isExtensionChanged(previousFileName, currentFileName) -> {
                isNameCurrentlyValid = false
                onValidationWarning.accept(context.getString(R.string.warn_rename_extension))
            }

            !isNameCurrentlyValid -> {
                isNameCurrentlyValid = true
                onValidationSuccess.run()
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
}
