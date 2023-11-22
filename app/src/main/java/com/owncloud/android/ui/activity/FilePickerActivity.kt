/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import com.owncloud.android.R
import com.owncloud.android.ui.fragment.OCFileListFragment

/**
 * File picker of remote files
 */
class FilePickerActivity : FolderPickerActivity() {

    override fun createFragments() {
        val listOfFiles = OCFileListFragment()
        val args = Bundle()
        args.putBoolean(OCFileListFragment.ARG_ONLY_FOLDERS_CLICKABLE, true)
        args.putBoolean(OCFileListFragment.ARG_HIDE_FAB, true)
        args.putBoolean(OCFileListFragment.ARG_HIDE_ITEM_OPTIONS, true)
        args.putBoolean(OCFileListFragment.ARG_SEARCH_ONLY_FOLDER, false)
        args.putBoolean(OCFileListFragment.ARG_FILE_SELECTABLE, true)
        args.putString(OCFileListFragment.ARG_MIMETYPE, intent.getStringExtra(OCFileListFragment.ARG_MIMETYPE))
        listOfFiles.arguments = args
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FOLDERS)
        transaction.commit()
    }
}
