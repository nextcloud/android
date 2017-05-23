/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
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
package com.owncloud.android.ui.fragment.contactsbackup;

import android.Manifest;
import android.accounts.Account;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.services.ContactsBackupJob;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP;
import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP;

public class ContactsBackupFragment extends FileFragment implements DatePickerDialog.OnDateSetListener {
    public static final String TAG = ContactsBackupFragment.class.getSimpleName();

    @BindView(R.id.contacts_automatic_backup)
    public SwitchCompat backupSwitch;

    @BindView(R.id.contacts_header_restore)
    public TextView contactsRestoreHeader;

    @BindView(R.id.contacts_datepicker)
    public AppCompatButton contactsDatePickerBtn;

    @BindView(R.id.contacts_last_backup_timestamp)
    public TextView lastBackup;

    private Date selectedDate = null;
    private boolean calendarPickerOpen;

    private DatePickerDialog datePickerDialog;

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    private static final String KEY_CALENDAR_PICKER_OPEN = "IS_CALENDAR_PICKER_OPEN";
    private static final String KEY_CALENDAR_DAY = "CALENDAR_DAY";
    private static final String KEY_CALENDAR_MONTH = "CALENDAR_MONTH";
    private static final String KEY_CALENDAR_YEAR = "CALENDAR_YEAR";
    private ArbitraryDataProvider arbitraryDataProvider;
    private Account account;


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.contacts_backup_fragment, null);
        ButterKnife.bind(this, view);

        setHasOptionsMenu(true);

        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        account = (Account) getArguments().get(ContactListFragment.ACCOUNT);

        contactsPreferenceActivity.getSupportActionBar().setTitle(R.string.actionbar_contacts);
        contactsPreferenceActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        backupSwitch.setChecked(arbitraryDataProvider.getBooleanValue(account, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP));

        onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkAndAskForContactsReadPermission()) {
                    if (isChecked) {
                        setAutomaticBackup(true);
                    } else {
                        setAutomaticBackup(false);
                    }
                }
            }
        };

        backupSwitch.setOnCheckedChangeListener(onCheckedChangeListener);

        // display last backup
        Long lastBackupTimestamp = arbitraryDataProvider.getLongValue(account, PREFERENCE_CONTACTS_LAST_BACKUP);

        if (lastBackupTimestamp == -1) {
            lastBackup.setText(R.string.contacts_preference_backup_never);
        } else {
            lastBackup.setText(DisplayUtils.getRelativeTimestamp(contactsPreferenceActivity, lastBackupTimestamp));
        }

        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_CALENDAR_PICKER_OPEN, false)) {
            if (savedInstanceState.getInt(KEY_CALENDAR_YEAR, -1) != -1 &&
                    savedInstanceState.getInt(KEY_CALENDAR_MONTH, -1) != -1 &&
                    savedInstanceState.getInt(KEY_CALENDAR_DAY, -1) != -1) {
                selectedDate = new Date(savedInstanceState.getInt(KEY_CALENDAR_YEAR),
                        savedInstanceState.getInt(KEY_CALENDAR_MONTH), savedInstanceState.getInt(KEY_CALENDAR_DAY));
            }
            calendarPickerOpen = true;
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (calendarPickerOpen) {
            if (selectedDate != null) {
                openDate(selectedDate);
            } else {
                openDate(null);
            }
        }

        ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        String backupFolderPath = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        refreshBackupFolder(backupFolderPath, contactsPreferenceActivity);
    }

    private void refreshBackupFolder(final String backupFolderPath,
                                     final ContactsPreferenceActivity contactsPreferenceActivity) {
        AsyncTask<String, Integer, Boolean> task = new AsyncTask<String, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(String... path) {
                FileDataStorageManager storageManager = new FileDataStorageManager(account,
                        getActivity().getContentResolver());

                RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR);
                try {
                    OCFile folder = storageManager.getFileByPath(path[0]);
                    RefreshFolderOperation operation = new RefreshFolderOperation(folder, System.currentTimeMillis(),
                            false, false, false, storageManager, account, getContext());

                    result = operation.execute(account, getContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return result.isSuccess();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    OCFile backupFolder = contactsPreferenceActivity.getStorageManager().getFileByPath(backupFolderPath);

                    Vector<OCFile> backupFiles = contactsPreferenceActivity.getStorageManager()
                            .getFolderContent(backupFolder, false);

                    if (backupFiles == null || backupFiles.size() == 0) {
                        contactsRestoreHeader.setVisibility(View.GONE);
                        contactsDatePickerBtn.setVisibility(View.GONE);
                    } else {
                        contactsRestoreHeader.setVisibility(View.VISIBLE);
                        contactsDatePickerBtn.setVisibility(View.VISIBLE);
                    }
                }
            }
        };

        task.execute(backupFolderPath);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (contactsPreferenceActivity.isDrawerOpen()) {
                    contactsPreferenceActivity.closeDrawer();
                } else {
                    contactsPreferenceActivity.openDrawer();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.READ_CONTACTS.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        setAutomaticBackup(true);
                    } else {
                        backupSwitch.setOnCheckedChangeListener(null);
                        backupSwitch.setChecked(false);
                        backupSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
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

    @OnClick(R.id.contacts_backup_now)
    public void backupContacts() {
        if (checkAndAskForContactsReadPermission()) {
            startContactsBackupJob();
        }
    }

    private void startContactsBackupJob() {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        PersistableBundleCompat bundle = new PersistableBundleCompat();
        bundle.putString(ContactsBackupJob.ACCOUNT, contactsPreferenceActivity.getAccount().name);
        bundle.putBoolean(ContactsBackupJob.FORCE, true);

        new JobRequest.Builder(ContactsBackupJob.TAG)
                .setExtras(bundle)
                .setExecutionWindow(3_000L, 10_000L)
                .setRequiresCharging(false)
                .setPersisted(false)
                .setUpdateCurrent(false)
                .build()
                .schedule();

        Snackbar.make(getView().findViewById(R.id.contacts_linear_layout),
                R.string.contacts_preferences_backup_scheduled,
                Snackbar.LENGTH_LONG).show();
    }

    private void setAutomaticBackup(final boolean bool) {

        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        if (bool) {
            ContactsPreferenceActivity.startContactBackupJob(contactsPreferenceActivity.getAccount());
        } else {
            ContactsPreferenceActivity.cancelContactBackupJobForAccount(contactsPreferenceActivity,
                    contactsPreferenceActivity.getAccount());
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(account, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                String.valueOf(bool));
    }

    private boolean checkAndAskForContactsReadPermission() {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        // check permissions
        if ((PermissionUtil.checkSelfPermission(contactsPreferenceActivity, Manifest.permission.READ_CONTACTS))) {
            return true;
        } else {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(contactsPreferenceActivity,
                    android.Manifest.permission.READ_CONTACTS)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(getView().findViewById(R.id.contacts_linear_layout),
                        R.string.contacts_read_permission,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                                        PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC);
                            }
                        });

                DisplayUtils.colorSnackbar(contactsPreferenceActivity, snackbar);

                snackbar.show();

                return false;
            } else {
                // No explanation needed, request the permission.
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                        PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC);
                return false;
            }
        }
    }

    @OnClick(R.id.contacts_datepicker)
    public void openCleanDate() {
        openDate(null);
    }

    public void openDate(@Nullable Date savedDate) {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        String backupFolderString = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        OCFile backupFolder = contactsPreferenceActivity.getStorageManager().getFileByPath(backupFolderString);

        Vector<OCFile> backupFiles = contactsPreferenceActivity.getStorageManager().getFolderContent(backupFolder,
                false);

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
        int year;
        int month;
        int day;

        if (savedDate == null) {
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH) + 1;
            day = cal.get(Calendar.DAY_OF_MONTH);
        } else {
            year = savedDate.getYear();
            month = savedDate.getMonth();
            day = savedDate.getDay();
        }

        if (backupFiles.size() > 0 && backupFiles.lastElement() != null) {
            datePickerDialog = new DatePickerDialog(contactsPreferenceActivity, this, year, month, day);
            datePickerDialog.getDatePicker().setMaxDate(backupFiles.lastElement().getModificationTimestamp());
            datePickerDialog.getDatePicker().setMinDate(backupFiles.firstElement().getModificationTimestamp());

            datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    selectedDate = null;
                }
            });

            datePickerDialog.show();
        } else {
            Toast.makeText(contactsPreferenceActivity, R.string.contacts_preferences_something_strange_happened,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (datePickerDialog != null) {
            datePickerDialog.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (datePickerDialog != null) {
            outState.putBoolean(KEY_CALENDAR_PICKER_OPEN, datePickerDialog.isShowing());

            if (datePickerDialog.isShowing()) {
                outState.putInt(KEY_CALENDAR_DAY, datePickerDialog.getDatePicker().getDayOfMonth());
                outState.putInt(KEY_CALENDAR_MONTH, datePickerDialog.getDatePicker().getMonth());
                outState.putInt(KEY_CALENDAR_YEAR, datePickerDialog.getDatePicker().getYear());
            }
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        selectedDate = new Date(year, month, dayOfMonth);

        String backupFolderString = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        OCFile backupFolder = contactsPreferenceActivity.getStorageManager().getFileByPath(backupFolderString);
        Vector<OCFile> backupFiles = contactsPreferenceActivity.getStorageManager().getFolderContent(
                backupFolder, false);

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
            Fragment contactListFragment = ContactListFragment.newInstance(backupToRestore,
                    contactsPreferenceActivity.getAccount());

            contactsPreferenceActivity.getSupportFragmentManager().
                    beginTransaction()
                    .replace(R.id.frame_container, contactListFragment, ContactListFragment.TAG)
                    .addToBackStack(ContactsPreferenceActivity.BACKUP_TO_LIST)
                    .commit();
        } else {
            Toast.makeText(contactsPreferenceActivity, R.string.contacts_preferences_no_file_found,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
