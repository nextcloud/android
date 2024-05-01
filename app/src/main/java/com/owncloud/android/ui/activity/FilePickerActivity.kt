/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
