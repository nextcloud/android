/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel

import java.util.ArrayList

/**
 * Business object representing a media folder with all information that are gathered via media queries.
 */
data class MediaFolder(
    /** name of the folder.  */
    @JvmField
    var folderName: String? = null,

    /** absolute path of the folder.  */
    @JvmField
    var absolutePath: String = "",

    /** list of file paths of the folder's content  */
    @JvmField
    var filePaths: List<String> = ArrayList(),

    /** total number of files in the media folder.  */
    @JvmField
    var numberOfFiles: Long = 0,

    /** type of media folder.  */
    @JvmField
    var type: MediaFolderType? = null
)
