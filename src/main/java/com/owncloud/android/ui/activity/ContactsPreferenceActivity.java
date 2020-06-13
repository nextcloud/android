/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactsBackupFragment;

import org.parceler.Parcels;

import javax.inject.Inject;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * This activity shows all settings for contact backup/restore
 */
public class ContactsPreferenceActivity extends FileActivity implements FileFragment.ContainerActivity {
    public static final String TAG = ContactsPreferenceActivity.class.getSimpleName();


    public static final String PREFERENCE_CONTACTS_AUTOMATIC_BACKUP = "PREFERENCE_CONTACTS_AUTOMATIC_BACKUP";
    public static final String PREFERENCE_CONTACTS_LAST_BACKUP = "PREFERENCE_CONTACTS_LAST_BACKUP";
    public static final String BACKUP_TO_LIST = "BACKUP_TO_LIST";
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";

    @Inject BackgroundJobManager backgroundJobManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contacts_preference);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_contacts);

        // show sidebar?
        boolean showSidebar = getIntent().getBooleanExtra(EXTRA_SHOW_SIDEBAR, true);
        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
            }
        }

        Intent intent = getIntent();
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (intent == null || intent.getParcelableExtra(ContactListFragment.FILE_NAME) == null ||
                    intent.getParcelableExtra(ContactListFragment.USER) == null) {
                ContactsBackupFragment fragment = new ContactsBackupFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean(EXTRA_SHOW_SIDEBAR, showSidebar);
                fragment.setArguments(bundle);
                transaction.add(R.id.frame_container, fragment);
            } else {
                OCFile file = Parcels.unwrap(intent.getParcelableExtra(ContactListFragment.FILE_NAME));
                User user = intent.getParcelableExtra(ContactListFragment.USER);
                ContactListFragment contactListFragment = ContactListFragment.newInstance(file, user);
                transaction.add(R.id.frame_container, contactListFragment);
            }
            transaction.commit();
        }
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }

    @Override
    public void showDetails(OCFile file) {
        // not needed
    }

    @Override
    public void showDetails(OCFile file, int activeTab) {
        // not needed
    }

    @Override
    public void onBrowsedDownTo(OCFile folder) {
        // not needed
    }

    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        // not needed
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().findFragmentByTag(ContactListFragment.TAG) != null) {
            getSupportFragmentManager().popBackStack(BACKUP_TO_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            finish();
        }
    }
}
