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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ContactsBackupFragmentBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeSnackbarUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import third_parties.daveKoeller.AlphanumComparator;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP;
import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP;

public class ContactsBackupFragment extends FileFragment implements DatePickerDialog.OnDateSetListener, Injectable {
    public static final String TAG = ContactsBackupFragment.class.getSimpleName();
    private static final String ARG_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    private static final String KEY_CALENDAR_PICKER_OPEN = "IS_CALENDAR_PICKER_OPEN";
    private static final String KEY_CALENDAR_DAY = "CALENDAR_DAY";
    private static final String KEY_CALENDAR_MONTH = "CALENDAR_MONTH";
    private static final String KEY_CALENDAR_YEAR = "CALENDAR_YEAR";

    private ContactsBackupFragmentBinding binding;

    @Inject BackgroundJobManager backgroundJobManager;

    private Date selectedDate;
    private boolean calendarPickerOpen;

    private DatePickerDialog datePickerDialog;

    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private ArbitraryDataProvider arbitraryDataProvider;
    private Account account;
    private boolean showSidebar = true;

    public static ContactsBackupFragment create(boolean showSidebar) {
        ContactsBackupFragment fragment = new ContactsBackupFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARG_SHOW_SIDEBAR, showSidebar);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // use grey as fallback for elements where custom theming is not available
        if (ThemeUtils.themingEnabled(getContext())) {
            getContext().getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }

        binding = ContactsBackupFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            showSidebar = getArguments().getBoolean(ARG_SHOW_SIDEBAR);
        }

        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        account = contactsPreferenceActivity.getAccount();
        User user = contactsPreferenceActivity.getUser().orElseThrow(RuntimeException::new);

        ActionBar actionBar = contactsPreferenceActivity != null ? contactsPreferenceActivity.getSupportActionBar() : null;

        if (actionBar != null) {
            ThemeToolbarUtils.setColoredTitle(actionBar, getString(R.string.actionbar_contacts), getContext());

            actionBar.setDisplayHomeAsUpEnabled(true);
            ThemeToolbarUtils.tintBackButton(actionBar, getContext());
        }

        arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        ThemeCheckableUtils.tintSwitch(
            binding.contactsAutomaticBackup, ThemeColorUtils.primaryAccentColor(getContext()));
        binding.contactsAutomaticBackup.setChecked(
            arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP));

        onCheckedChangeListener = (buttonView, isChecked) -> {
            if (checkAndAskForContactsReadPermission()) {
                setAutomaticBackup(isChecked);
            }
        };

        binding.contactsAutomaticBackup.setOnCheckedChangeListener(onCheckedChangeListener);
        binding.contactsBackupNow.setOnClickListener(v -> backupContacts());
        binding.contactsDatepicker.setOnClickListener(v -> openCleanDate());

        // display last backup
        Long lastBackupTimestamp = arbitraryDataProvider.getLongValue(user, PREFERENCE_CONTACTS_LAST_BACKUP);

        if (lastBackupTimestamp == -1) {
            binding.contactsLastBackupTimestamp.setText(R.string.contacts_preference_backup_never);
        } else {
            binding.contactsLastBackupTimestamp.setText(
                DisplayUtils.getRelativeTimestamp(contactsPreferenceActivity, lastBackupTimestamp));
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

        ThemeButtonUtils.colorPrimaryButton(binding.contactsBackupNow, getContext());
        ThemeButtonUtils.colorPrimaryButton(binding.contactsDatepicker, getContext());

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

        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        if (contactsPreferenceActivity != null) {
            String backupFolderPath = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
            refreshBackupFolder(backupFolderPath, contactsPreferenceActivity, contactsPreferenceActivity.getStorageManager());
        }
    }

    private void refreshBackupFolder(final String backupFolderPath,
                                     final Context context,
                                     final FileDataStorageManager storageManager) {
        AsyncTask<String, Integer, Boolean> task = new AsyncTask<String, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(String... path) {
                OCFile folder = storageManager.getFileByPath(path[0]);

                if (folder != null) {
                    RefreshFolderOperation operation = new RefreshFolderOperation(folder, System.currentTimeMillis(),
                            false, false, storageManager, account, getContext());

                    RemoteOperationResult result = operation.execute(account, context);
                    return result.isSuccess();
                } else {
                    return Boolean.FALSE;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    OCFile backupFolder = storageManager.getFileByPath(backupFolderPath);

                    List<OCFile> backupFiles = storageManager
                            .getFolderContent(backupFolder, false);

                    Collections.sort(backupFiles, new AlphanumComparator<>());

                    if (backupFiles == null || backupFiles.isEmpty()) {
                        binding.contactsDatepicker.setVisibility(View.GONE);
                    } else {
                        binding.contactsDatepicker.setVisibility(View.VISIBLE);
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
                if (showSidebar) {
                    if (contactsPreferenceActivity.isDrawerOpen()) {
                        contactsPreferenceActivity.closeDrawer();
                    } else {
                        contactsPreferenceActivity.openDrawer();
                    }
                } else {
                    Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
                    startActivity(settingsIntent);
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
                        binding.contactsAutomaticBackup.setOnCheckedChangeListener(null);
                        binding.contactsAutomaticBackup.setChecked(false);
                        binding.contactsAutomaticBackup.setOnCheckedChangeListener(onCheckedChangeListener);
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

    public void backupContacts() {
        if (checkAndAskForContactsReadPermission()) {
            startContactsBackupJob();
        }
    }

    private void startContactsBackupJob() {
        ContactsPreferenceActivity activity = (ContactsPreferenceActivity)getActivity();
        if (activity != null) {
            Optional<User> optionalUser = activity.getUser();
            if (optionalUser.isPresent()) {
                backgroundJobManager.startImmediateContactsBackup(optionalUser.get());
                DisplayUtils.showSnackMessage(getView().findViewById(R.id.contacts_linear_layout),
                                              R.string.contacts_preferences_backup_scheduled);
            }
        }
    }

    private void setAutomaticBackup(final boolean enabled) {

        final ContactsPreferenceActivity activity = (ContactsPreferenceActivity) getActivity();
        if (activity == null) {
            return;
        }
        Optional<User> optionalUser = activity.getUser();
        if (!optionalUser.isPresent()) {
            return;
        }
        User user = optionalUser.get();
        if (enabled) {
            backgroundJobManager.schedulePeriodicContactsBackup(user);
        } else {
            backgroundJobManager.cancelPeriodicContactsBackup(user);
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                String.valueOf(enabled));
    }

    private boolean checkAndAskForContactsReadPermission() {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        // check permissions
        if (PermissionUtil.checkSelfPermission(contactsPreferenceActivity, Manifest.permission.READ_CONTACTS)) {
            return true;
        } else {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(contactsPreferenceActivity,
                    android.Manifest.permission.READ_CONTACTS)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = DisplayUtils.createSnackbar(
                        getView().findViewById(R.id.contacts_linear_layout),
                        R.string.contacts_read_permission, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_ok, v -> requestPermissions(
                                new String[]{Manifest.permission.READ_CONTACTS},
                                PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC)
                        );

                ThemeSnackbarUtils.colorSnackbar(contactsPreferenceActivity, snackbar);

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

    public void openCleanDate() {
        openDate(null);
    }

    public void openDate(@Nullable Date savedDate) {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        String backupFolderString = getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        OCFile backupFolder = contactsPreferenceActivity.getStorageManager().getFileByPath(backupFolderString);

        List<OCFile> backupFiles = contactsPreferenceActivity.getStorageManager().getFolderContent(backupFolder,
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

        if (backupFiles.size() > 0 && backupFiles.get(backupFiles.size() - 1) != null) {
            datePickerDialog = new DatePickerDialog(contactsPreferenceActivity, this, year, month, day);
            datePickerDialog.getDatePicker().setMaxDate(backupFiles.get(backupFiles.size() - 1)
                    .getModificationTimestamp());
            datePickerDialog.getDatePicker().setMinDate(backupFiles.get(0).getModificationTimestamp());

            datePickerDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    selectedDate = null;
                }
            });

            datePickerDialog.setTitle("");
            datePickerDialog.show();

            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ThemeColorUtils.primaryColor(getContext(),true));
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ThemeColorUtils.primaryColor(getContext(), true));

            // set background to transparent
            datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setBackgroundColor(0x00000000);
            datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setBackgroundColor(0x00000000);
        } else {
            DisplayUtils.showSnackMessage(getView().findViewById(R.id.contacts_linear_layout),
                    R.string.contacts_preferences_something_strange_happened);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (datePickerDialog != null) {
            datePickerDialog.dismiss();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
        List<OCFile> backupFiles = contactsPreferenceActivity.getStorageManager().getFolderContent(
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
            final User user = contactsPreferenceActivity.getUser().orElseThrow(RuntimeException::new);
            Fragment contactListFragment = ContactListFragment.newInstance(backupToRestore, user);

            contactsPreferenceActivity.getSupportFragmentManager().
                    beginTransaction()
                    .replace(R.id.frame_container, contactListFragment, ContactListFragment.TAG)
                    .addToBackStack(ContactsPreferenceActivity.BACKUP_TO_LIST)
                    .commit();
        } else {
            DisplayUtils.showSnackMessage(getView().findViewById(R.id.contacts_linear_layout),
                    R.string.contacts_preferences_no_file_found);
        }
    }
}
