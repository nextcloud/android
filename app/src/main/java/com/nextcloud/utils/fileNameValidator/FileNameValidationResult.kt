/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import com.owncloud.android.R

enum class FileNameValidationResult(val messageId: Int) {
    EMPTY(R.string.filename_empty),
    INVALID_CHARACTER(R.string.file_name_validator_error_invalid_character),
    RESERVED_NAME(R.string.file_name_validator_error_reserved_names),
    ENDS_WITH_SPACE_OR_PERIOD(R.string.file_name_validator_error_ends_with_space_period)
}
