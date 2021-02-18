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

package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.view.View;

import com.owncloud.android.R;
import com.owncloud.android.ui.fragment.OCFileListFragment;

import androidx.fragment.app.FragmentTransaction;

/**
 * File picker of remote files
 */
public class FilePickerActivity extends FolderPickerActivity {

    @Override
    public void onClick(View v) {
        super.onClick(v);
    }

    @Override
    protected void createFragments() {
        OCFileListFragment listOfFiles = new OCFileListFragment();
        Bundle args = new Bundle();
        args.putBoolean(OCFileListFragment.ARG_ONLY_FOLDERS_CLICKABLE, true);
        args.putBoolean(OCFileListFragment.ARG_HIDE_FAB, true);
        args.putBoolean(OCFileListFragment.ARG_HIDE_ITEM_OPTIONS, true);
        args.putBoolean(OCFileListFragment.ARG_SEARCH_ONLY_FOLDER, false);
        args.putBoolean(OCFileListFragment.ARG_FILE_SELECTABLE, true);
        args.putString(OCFileListFragment.ARG_MIMETYPE, getIntent().getStringExtra(OCFileListFragment.ARG_MIMETYPE));
        listOfFiles.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FOLDERS);
        transaction.commit();
    }
}
