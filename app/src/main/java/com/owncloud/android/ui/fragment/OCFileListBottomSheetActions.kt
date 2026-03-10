/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import com.owncloud.android.lib.common.Creator

interface OCFileListBottomSheetActions {
    fun createFolder(encrypted: Boolean)

    /**
     * offers a file upload with the Android OS file picker to the current folder.
     */
    fun uploadFromApp()

    /**
     * offers a file upload with the app file picker to the current folder.
     */
    fun uploadFiles()

    /**
     * opens template selection for documents
     */
    fun newDocument()

    /**
     * opens template selection for spreadsheets
     */
    fun newSpreadsheet()

    fun newPresentation()
    fun directCameraUpload()
    fun scanDocUpload()

    /**
     * Offers scanning a document in a supported external app and then upload to the current folder.
     */
    fun scanDocUploadFromApp()

    /**
     * @return true, if a supported external app is available for [.scanDocUploadFromApp]
     */
    val isScanDocUploadFromAppAvailable: Boolean

    /**
     * open template selection for creator @link Creator
     */
    fun showTemplate(creator: Creator?, headline: String?)

    fun createRichWorkspace()
}
