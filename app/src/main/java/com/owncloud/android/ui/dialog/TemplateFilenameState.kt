/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

sealed class TemplateFilenameState(val errorMessage: CharSequence?) {
    data object Valid : TemplateFilenameState(null)
    data object NoTemplateSelected : TemplateFilenameState(null)
    class JustExtension(message: CharSequence) : TemplateFilenameState(message)
    class HiddenName(message: CharSequence) : TemplateFilenameState(message)
    class ChangedExtension(message: CharSequence) : TemplateFilenameState(message)
    class Invalid(message: CharSequence) : TemplateFilenameState(message)
}
