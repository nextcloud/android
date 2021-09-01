/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import com.nextcloud.client.account.User;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * File details pager adapter.
 */
public class FileDetailTabAdapter extends FragmentStatePagerAdapter {
    private OCFile file;
    private User user;

    private FileDetailSharingFragment fileDetailSharingFragment;
    private FileDetailActivitiesFragment fileDetailActivitiesFragment;

    public FileDetailTabAdapter(FragmentManager fm, OCFile file, User user) {
        super(fm);
        this.file = file;
        this.user = user;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
            default:
                fileDetailActivitiesFragment = FileDetailActivitiesFragment.newInstance(file, user);
                return fileDetailActivitiesFragment;
            case 1:
                fileDetailSharingFragment = FileDetailSharingFragment.newInstance(file, user);
                return fileDetailSharingFragment;
        }
    }

    public FileDetailSharingFragment getFileDetailSharingFragment() {
        return fileDetailSharingFragment;
    }

    public FileDetailActivitiesFragment getFileDetailActivitiesFragment() {
        return fileDetailActivitiesFragment;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
