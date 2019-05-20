/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
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

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.jobs.ContactsBackupJob;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactsBackupFragment;
import com.owncloud.android.utils.DisplayUtils;

import org.parceler.Parcels;

import java.util.Set;

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
                    intent.getParcelableExtra(ContactListFragment.ACCOUNT) == null) {
                ContactsBackupFragment fragment = new ContactsBackupFragment();
                Bundle bundle = new Bundle();
                bundle.putBoolean(EXTRA_SHOW_SIDEBAR, showSidebar);
                fragment.setArguments(bundle);
                transaction.add(R.id.frame_container, fragment);
            } else {
                OCFile file = Parcels.unwrap(intent.getParcelableExtra(ContactListFragment.FILE_NAME));
                Account account = Parcels.unwrap(intent.getParcelableExtra(ContactListFragment.ACCOUNT));
                ContactListFragment contactListFragment = ContactListFragment.newInstance(file, account);
                transaction.add(R.id.frame_container, contactListFragment);
            }
            transaction.commit();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(
                getUserAccountManager().getCurrentAccount(),
                bottomNavigationView,
                getResources(),
                accountManager,
                this,
                -1
            );
        }
    }

    public static void startContactBackupJob(Account account) {
        Log_OC.d(TAG, "start daily contacts backup job");

        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsBackupJob.ACCOUNT, account.name);

        cancelPreviousContactBackupJobForAccount(MainApp.getAppContext(), account);

        new JobRequest.Builder(ContactsBackupJob.TAG)
                .setExtras(bundle)
                .setPeriodic(24 * 60 * 60 * 1000)
                .build()
                .schedule();
    }

    public static void cancelPreviousContactBackupJobForAccount(Context context, Account account) {
        Log_OC.d(TAG, "disabling existing contacts backup job for account: " + account.name);

        JobManager jobManager = JobManager.create(context);
        Set<JobRequest> jobs = jobManager.getAllJobRequestsForTag(ContactsBackupJob.TAG);

        for (JobRequest jobRequest : jobs) {
            PersistableBundleCompat extras = jobRequest.getExtras();
            if (extras.getString(ContactsBackupJob.ACCOUNT, "").equalsIgnoreCase(account.name) &&
                    jobRequest.isPeriodic()) {
                jobManager.cancel(jobRequest.getJobId());
            }
        }
    }

    public static void cancelContactBackupJobForAccount(Context context, Account account) {
        Log_OC.d(TAG, "disabling contacts backup job for account: " + account.name);

        JobManager jobManager = JobManager.create(context);
        Set<JobRequest> jobs = jobManager.getAllJobRequestsForTag(ContactsBackupJob.TAG);

        for (JobRequest jobRequest : jobs) {
            PersistableBundleCompat extras = jobRequest.getExtras();
            if (extras.getString(ContactsBackupJob.ACCOUNT, "").equalsIgnoreCase(account.name)) {
                jobManager.cancel(jobRequest.getJobId());
            }
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
