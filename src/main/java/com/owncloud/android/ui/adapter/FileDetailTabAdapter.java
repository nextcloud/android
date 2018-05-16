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

import android.accounts.Account;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;

/**
 * File details pager adapter.
 */
public class FileDetailTabAdapter extends FragmentStatePagerAdapter {
    private OCFile file;
    private Account account;

    private FileDetailSharingFragment fileDetailSharingFragment;

    public FileDetailTabAdapter(FragmentManager fm, OCFile file, Account account) {
        super(fm);

        this.file = file;
        this.account = account;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return FileDetailActivitiesFragment.newInstance(file, account);
            case 1:
                fileDetailSharingFragment = FileDetailSharingFragment.newInstance(file, account);
                return fileDetailSharingFragment;
            default:
                return null;
        }
    }

    public FileDetailSharingFragment getFileDetailSharingFragment() {
        return fileDetailSharingFragment;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
