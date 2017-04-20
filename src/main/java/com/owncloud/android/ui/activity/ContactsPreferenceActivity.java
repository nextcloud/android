/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.ContactsBackupJob;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.Vector;

/**
 * This activity shows all settings for contact backup/restore
 */

public class ContactsPreferenceActivity extends FileActivity implements FileFragment.ContainerActivity {
    public static final String TAG = ContactsPreferenceActivity.class.getSimpleName();

    public static final String PREFERENCE_CONTACTS_AUTOMATIC_BACKUP = "PREFERENCE_CONTACTS_AUTOMATIC_BACKUP";
    public static final String PREFERENCE_CONTACTS_LAST_BACKUP = "PREFERENCE_CONTACTS_LAST_BACKUP";

    private SwitchCompat backupSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contacts_preference);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_contacts);

        getSupportActionBar().setTitle(R.string.actionbar_contacts);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        backupSwitch = (SwitchCompat) findViewById(R.id.contacts_automatic_backup);
        backupSwitch.setChecked(sharedPreferences.getBoolean(PREFERENCE_CONTACTS_AUTOMATIC_BACKUP, false));

        backupSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked &&
                        checkAndAskForContactsReadPermission(PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC)) {
                    // store value
                    setAutomaticBackup(backupSwitch, true);

                    // enable daily job
                    startContactBackupJob(getAccount());
                } else {
                    setAutomaticBackup(backupSwitch, false);

                    // cancel pending jobs
                    cancelContactBackupJob(getBaseContext());
                }
            }
        });

        // display last backup
        TextView lastBackup = (TextView) findViewById(R.id.contacts_last_backup_timestamp);
        Long lastBackupTimestamp = sharedPreferences.getLong(PREFERENCE_CONTACTS_LAST_BACKUP, -1);

        if (lastBackupTimestamp == -1) {
            lastBackup.setText(R.string.contacts_preference_backup_never);
        } else {
            lastBackup.setText(DisplayUtils.getRelativeTimestamp(getBaseContext(), lastBackupTimestamp));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.READ_CONTACTS.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        setAutomaticBackup(backupSwitch, true);
                    } else {
                        setAutomaticBackup(backupSwitch, false);
                    }

                    break;
                }
            }
        }

        if (requestCode == PermissionUtil.PERMISSIONS_READ_CONTACTS_MANUALLY) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.READ_CONTACTS.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        startContactsBackupJob();
                    }

                    break;
                }
            }
        }
    }

    public void backupContacts(View v) {
        if (checkAndAskForContactsReadPermission(PermissionUtil.PERMISSIONS_READ_CONTACTS_MANUALLY)) {
            startContactsBackupJob();
        }
    }

    private void startContactsBackupJob() {
        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsBackupJob.ACCOUNT, getAccount().name);
        bundle.putBoolean(ContactsBackupJob.FORCE, true);

        new JobRequest.Builder(ContactsBackupJob.TAG)
                .setExtras(bundle)
                .setExecutionWindow(3_000L, 10_000L)
                .setRequiresCharging(false)
                .setPersisted(false)
                .setUpdateCurrent(false)
                .build()
                .schedule();

        Snackbar.make(findViewById(R.id.contacts_linear_layout), R.string.contacts_preferences_backup_scheduled,
                Snackbar.LENGTH_LONG).show();
    }

    private void setAutomaticBackup(SwitchCompat backupSwitch, boolean bool) {
        backupSwitch.setChecked(bool);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCE_CONTACTS_AUTOMATIC_BACKUP, bool);
        editor.apply();
    }

    private boolean checkAndAskForContactsReadPermission(final int permission) {
        // check permissions
        if ((PermissionUtil.checkSelfPermission(this, Manifest.permission.READ_CONTACTS))) {
            return true;
        } else {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(ContactsPreferenceActivity.this,
                    android.Manifest.permission.READ_CONTACTS)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(findViewById(R.id.contacts_linear_layout), R.string.contacts_read_permission,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PermissionUtil.requestReadContactPermission(ContactsPreferenceActivity.this, permission);
                            }
                        });

                DisplayUtils.colorSnackbar(this, snackbar);

                snackbar.show();

                return false;
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestReadContactPermission(ContactsPreferenceActivity.this, permission);

                return false;
            }
        }
    }

    public void openDate(View v) {
        String backupFolderString = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        OCFile backupFolder = getStorageManager().getFileByPath(backupFolderString);

        Vector<OCFile> backupFiles = getStorageManager().getFolderContent(backupFolder, false);

        Collections.sort(backupFiles, new Comparator<OCFile>() {
            @Override
            public int compare(OCFile o1, OCFile o2) {
                if (o1.getModificationTimestamp() == o2.getModificationTimestamp()) {
                    return 0;
                }

                if (o1.getModificationTimestamp() > o2.getModificationTimestamp()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                String backupFolderString = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
                OCFile backupFolder = getStorageManager().getFileByPath(backupFolderString);
                Vector<OCFile> backupFiles = getStorageManager().getFolderContent(backupFolder, false);

                // find file with modification with date and time between 00:00 and 23:59
                // if more than one file exists, take oldest
                Calendar date = Calendar.getInstance();
                date.set(year, month, dayOfMonth);

                // start
                date.set(Calendar.HOUR, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 1);
                date.set(Calendar.MILLISECOND, 0);
                date.set(Calendar.AM_PM, Calendar.AM);
                Long start = date.getTimeInMillis();

                // end
                date.set(Calendar.HOUR, 23);
                date.set(Calendar.MINUTE, 59);
                date.set(Calendar.SECOND, 59);
                Long end = date.getTimeInMillis();

                OCFile backupToRestore = null;

                for (OCFile file : backupFiles) {
                    if (start < file.getModificationTimestamp() && end > file.getModificationTimestamp()) {
                        if (backupToRestore == null) {
                            backupToRestore = file;
                        } else if (backupToRestore.getModificationTimestamp() < file.getModificationTimestamp()) {
                            backupToRestore = file;
                        }
                    }
                }

                if (backupToRestore != null) {
                    Fragment contactListFragment = ContactListFragment.newInstance(backupToRestore, getAccount());

                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.contacts_linear_layout, contactListFragment);
                    transaction.commit();
                } else {
                    Toast.makeText(ContactsPreferenceActivity.this, R.string.contacts_preferences_no_file_found,
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, dateSetListener, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(backupFiles.lastElement().getModificationTimestamp());
        datePickerDialog.getDatePicker().setMinDate(backupFiles.firstElement().getModificationTimestamp());

        datePickerDialog.show();
    }

    public static void startContactBackupJob(Account account) {
        Log_OC.d(TAG, "start daily contacts backup job");

        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsBackupJob.ACCOUNT, account.name);

        new JobRequest.Builder(ContactsBackupJob.TAG)
                .setExtras(bundle)
                .setRequiresCharging(false)
                .setPersisted(true)
                .setUpdateCurrent(true)
                .setPeriodic(24 * 60 * 60 * 1000)
                .build()
                .schedule();
    }

    public static void cancelContactBackupJob(Context context) {
        Log_OC.d(TAG, "disabling contacts backup job");

        JobManager jobManager = JobManager.create(context);
        Set<JobRequest> jobs = jobManager.getAllJobRequestsForTag(ContactsBackupJob.TAG);

        for (JobRequest jobRequest : jobs) {
            jobManager.cancel(jobRequest.getJobId());
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                retval = true;
                break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
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
    public void onBrowsedDownTo(OCFile folder) {
        // not needed
    }

    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        // not needed
    }
}