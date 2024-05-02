/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

/**
 * Business object representing a media folder with all information that are gathered via media queries.
 */
class MediaFolder {
    /** name of the folder.  */
    @JvmField
    var folderName: String? = null

    /** absolute path of the folder.  */
    @JvmField
    var absolutePath: String? = null

    /** list of file paths of the folder's content  */
    @JvmField
    var filePaths: List<String> = ArrayList()

    /** total number of files in the media folder.  */
    @JvmField
    var numberOfFiles: Long = 0

    /** type of media folder.  */
    @JvmField
    var type: MediaFolderType? = null
}
