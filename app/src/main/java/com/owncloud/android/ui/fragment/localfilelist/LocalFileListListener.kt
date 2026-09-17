/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.localfilelist

import java.io.File

interface LocalFileListListener {
    fun onDirectoryClick(directory: File?)
    fun onFileClick(file: File?)
    val initialDirectory: File?
    val isFolderPickerMode: Boolean
    val isWithinEncryptedFolder: Boolean
}
