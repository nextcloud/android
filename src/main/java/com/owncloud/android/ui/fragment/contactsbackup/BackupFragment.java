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
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.databinding.BackupFragmentBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeCheckableUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

public class BackupFragment extends FileFragment implements DatePickerDialog.OnDateSetListener, Injectable {
    public static final String TAG = BackupFragment.class.getSimpleName();
    private static final String ARG_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    private static final String KEY_CALENDAR_PICKER_OPEN = "IS_CALENDAR_PICKER_OPEN";
    private static final String KEY_CALENDAR_DAY = "CALENDAR_DAY";
    private static final String KEY_CALENDAR_MONTH = "CALENDAR_MONTH";
    private static final String KEY_CALENDAR_YEAR = "CALENDAR_YEAR";

    private BackupFragmentBinding binding;

    @Inject BackgroundJobManager backgroundJobManager;

    private Date selectedDate;
    private boolean calendarPickerOpen;

    private DatePickerDialog datePickerDialog;

    private CompoundButton.OnCheckedChangeListener dailyBackupCheckedChangeListener;
    private CompoundButton.OnCheckedChangeListener contactsCheckedListener;
    private CompoundButton.OnCheckedChangeListener calendarCheckedListener;
    private ArbitraryDataProvider arbitraryDataProvider;
    private User user;
    private boolean showSidebar = true;

    public static BackupFragment create(boolean showSidebar) {
        BackupFragment fragment = new BackupFragment();
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

        binding = BackupFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setHasOptionsMenu(true);

        if (getArguments() != null) {
            showSidebar = getArguments().getBoolean(ARG_SHOW_SIDEBAR);
        }

        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();
        user = contactsPreferenceActivity.getUser().orElseThrow(RuntimeException::new);

        ActionBar actionBar = contactsPreferenceActivity != null ? contactsPreferenceActivity.getSupportActionBar() : null;

        if (actionBar != null) {
            ThemeToolbarUtils.setColoredTitle(actionBar, getString(R.string.backup_title), getContext());

            actionBar.setDisplayHomeAsUpEnabled(true);
            ThemeToolbarUtils.tintBackButton(actionBar, getContext());
        }

        arbitraryDataProvider = new ArbitraryDataProvider(getContext().getContentResolver());

        ThemeCheckableUtils.tintSwitch(
            binding.contacts, ThemeColorUtils.primaryAccentColor(getContext()));
        ThemeCheckableUtils.tintSwitch(
            binding.calendar, ThemeColorUtils.primaryAccentColor(getContext()));
        ThemeCheckableUtils.tintSwitch(
            binding.dailyBackup, ThemeColorUtils.primaryAccentColor(getContext()));
        binding.dailyBackup.setChecked(
            arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CONTACTS_AUTOMATIC_BACKUP));

        binding.contacts.setChecked(checkContactBackupPermission());
        binding.calendar.setChecked(checkCalendarBackupPermission());

        dailyBackupCheckedChangeListener = (buttonView, isChecked) -> {
            if (checkAndAskForContactsReadPermission()) {
                setAutomaticBackup(isChecked);
            }
        };

        contactsCheckedListener = (buttonView, isChecked) -> {
            if (isChecked) {
                if (checkAndAskForContactsReadPermission()) {
                    binding.backupNow.setVisibility(View.VISIBLE);
                }
            } else {
                if (!binding.calendar.isChecked()) {
                    binding.backupNow.setVisibility(View.INVISIBLE);
                }
            }
        };
        binding.contacts.setOnCheckedChangeListener(contactsCheckedListener);

        calendarCheckedListener = (buttonView, isChecked) -> {
            if (isChecked) {
                if (checkAndAskForCalendarReadPermission()) {
                    binding.backupNow.setVisibility(View.VISIBLE);
                }
            } else {
                if (!binding.contacts.isChecked()) {
                    binding.backupNow.setVisibility(View.INVISIBLE);
                }
            }
        };

        binding.calendar.setOnCheckedChangeListener(calendarCheckedListener);

        binding.dailyBackup.setOnCheckedChangeListener(dailyBackupCheckedChangeListener);
        binding.backupNow.setOnClickListener(v -> backup());
        binding.backupNow.setEnabled(checkBackupNowPermission());
        binding.backupNow.setVisibility(checkBackupNowPermission() ? View.VISIBLE : View.GONE);

        binding.contactsDatepicker.setOnClickListener(v -> openCleanDate());

        // display last backup
        Long lastBackupTimestamp = arbitraryDataProvider.getLongValue(user, PREFERENCE_CONTACTS_LAST_BACKUP);

        if (lastBackupTimestamp == -1) {
            binding.lastBackupWithDate.setVisibility(View.GONE);
        } else {
            binding.lastBackupWithDate.setText(
                String.format(getString(R.string.last_backup),
                              DisplayUtils.getRelativeTimestamp(contactsPreferenceActivity, lastBackupTimestamp)));
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

        ThemeButtonUtils.colorPrimaryButton(binding.backupNow, getContext());
        ThemeButtonUtils.themeBorderlessButton(binding.contactsDatepicker);

        int primaryAccentColor = ThemeColorUtils.primaryAccentColor(getContext());
        binding.dataToBackUpTitle.setTextColor(primaryAccentColor);
        binding.backupSettingsTitle.setTextColor(primaryAccentColor);

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
            refreshBackupFolder(backupFolderPath,
                                contactsPreferenceActivity.getApplicationContext(),
                                contactsPreferenceActivity.getStorageManager());
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
                            false, false, storageManager, user, context);

                    RemoteOperationResult result = operation.execute(user.toPlatformAccount(), context);
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
                } else if (getActivity() != null) {
                    getActivity().finish();
                } else {
                    Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
                    settingsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                        // if approved, exit for loop
                        break;
                    }

                    // if not accepted, disable again
                    binding.contacts.setOnCheckedChangeListener(null);
                    binding.contacts.setChecked(false);
                    binding.contacts.setOnCheckedChangeListener(contactsCheckedListener);
                }
            }
        }

        if (requestCode == PermissionUtil.PERMISSIONS_READ_CALENDAR_AUTOMATIC) {
            for (int index = 0; index < permissions.length; index++) {
                if (Manifest.permission.READ_CALENDAR.equalsIgnoreCase(permissions[index])) {
                    if (grantResults[index] >= 0) {
                        // if approved, exit for loop
                        break;
                    }
                }

                // if not accepted, disable again
                binding.calendar.setOnCheckedChangeListener(null);
                binding.calendar.setChecked(false);
                binding.calendar.setOnCheckedChangeListener(calendarCheckedListener);

                binding.backupNow.setVisibility(checkBackupNowPermission() ? View.VISIBLE : View.GONE);
            }
        }

        binding.backupNow.setVisibility(checkBackupNowPermission() ? View.VISIBLE : View.GONE);
        binding.backupNow.setEnabled(checkBackupNowPermission());
    }

    public void backup() {
        if (binding.contacts.isChecked() && checkAndAskForContactsReadPermission()) {
            startContactsBackupJob();
        }

        if (binding.calendar.isChecked() && checkAndAskForCalendarReadPermission()) {
            startCalendarBackupJob();
        }

        DisplayUtils.showSnackMessage(requireView().findViewById(R.id.contacts_linear_layout),
                                      R.string.contacts_preferences_backup_scheduled);
    }

    private void startContactsBackupJob() {
        ContactsPreferenceActivity activity = (ContactsPreferenceActivity) getActivity();
        if (activity != null) {
            Optional<User> optionalUser = activity.getUser();
            if (optionalUser.isPresent()) {
                backgroundJobManager.startImmediateContactsBackup(optionalUser.get());
            }
        }
    }

    private void startCalendarBackupJob() {
        ContactsPreferenceActivity activity = (ContactsPreferenceActivity) getActivity();
        if (activity != null) {
            Optional<User> optionalUser = activity.getUser();
            if (optionalUser.isPresent()) {
                backgroundJobManager.startImmediateCalendarBackup(optionalUser.get());
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
            backgroundJobManager.schedulePeriodicCalendarBackup(user);
        } else {
            backgroundJobManager.cancelPeriodicContactsBackup(user);
            backgroundJobManager.cancelPeriodicCalendarBackup(user);
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(), 
                                                    PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                                                    String.valueOf(enabled));
    }

    private boolean checkAndAskForContactsReadPermission() {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        // check permissions
        if (PermissionUtil.checkSelfPermission(contactsPreferenceActivity, Manifest.permission.READ_CONTACTS)) {
            return true;
        } else {
            // No explanation needed, request the permission.
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                               PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC);
            return false;
        }
    }

    private boolean checkAndAskForCalendarReadPermission() {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        // check permissions
        if (PermissionUtil.checkSelfPermission(contactsPreferenceActivity, Manifest.permission.READ_CALENDAR)) {
            return true;
        } else {
            // No explanation needed, request the permission.
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                               PermissionUtil.PERMISSIONS_READ_CALENDAR_AUTOMATIC);
            return false;
        }
    }

    private boolean checkBackupNowPermission() {
        return (checkCalendarBackupPermission() && binding.calendar.isChecked()) ||
            (checkContactBackupPermission() && binding.contacts.isChecked());
    }

    private boolean checkCalendarBackupPermission() {
        return PermissionUtil.checkSelfPermission(getContext(), Manifest.permission.READ_CALENDAR);
    }

    private boolean checkContactBackupPermission() {
        return PermissionUtil.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS);
    }

    public void openCleanDate() {
        if (checkAndAskForCalendarReadPermission() && checkAndAskForContactsReadPermission()) {
            openDate(null);
        }
    }

    public void openDate(@Nullable Date savedDate) {
        final ContactsPreferenceActivity contactsPreferenceActivity = (ContactsPreferenceActivity) getActivity();

        if (contactsPreferenceActivity == null) {
            Toast.makeText(getContext(), getString(R.string.error_choosing_date), Toast.LENGTH_LONG).show();
            return;
        }

        String contactsBackupFolderString =
            getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        String calendarBackupFolderString =
            getResources().getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR;

        FileDataStorageManager storageManager = contactsPreferenceActivity.getStorageManager();

        OCFile contactsBackupFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderString);
        OCFile calendarBackupFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderString);

        List<OCFile> backupFiles = storageManager.getFolderContent(contactsBackupFolder, false);
        backupFiles.addAll(storageManager.getFolderContent(calendarBackupFolder, false));

        Collections.sort(backupFiles, (o1, o2) -> {
            return Long.compare(o1.getModificationTimestamp(), o2.getModificationTimestamp());
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

            datePickerDialog.setOnDismissListener(dialog -> selectedDate = null);

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

        if (contactsPreferenceActivity == null) {
            Toast.makeText(getContext(), getString(R.string.error_choosing_date), Toast.LENGTH_LONG).show();
            return;
        }

        selectedDate = new Date(year, month, dayOfMonth);

        String contactsBackupFolderString =
            getResources().getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR;
        String calendarBackupFolderString =
            getResources().getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR;

        FileDataStorageManager storageManager = contactsPreferenceActivity.getStorageManager();

        OCFile contactsBackupFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderString);
        OCFile calendarBackupFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderString);

        List<OCFile> backupFiles = storageManager.getFolderContent(contactsBackupFolder, false);
        backupFiles.addAll(storageManager.getFolderContent(calendarBackupFolder, false));

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
        long start = date.getTimeInMillis();

        // end
        date.set(Calendar.HOUR, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        long end = date.getTimeInMillis();

        OCFile contactsBackupToRestore = null;
        List<OCFile> calendarBackupsToRestore = new ArrayList<>();

        for (OCFile file : backupFiles) {
            if (start < file.getModificationTimestamp() && end > file.getModificationTimestamp()) {
                // contact
                if (MimeTypeUtil.isVCard(file)) {
                    if (contactsBackupToRestore == null) {
                        contactsBackupToRestore = file;
                    } else if (contactsBackupToRestore.getModificationTimestamp() < file.getModificationTimestamp()) {
                        contactsBackupToRestore = file;
                    }
                }

                // calendars
                if (MimeTypeUtil.isCalendar(file)) {
                    calendarBackupsToRestore.add(file);
                }
            }
        }

        List<OCFile> backupToRestore = new ArrayList<>();

        if (contactsBackupToRestore != null) {
            backupToRestore.add(contactsBackupToRestore);
        }

        backupToRestore.addAll(calendarBackupsToRestore);


        if (backupToRestore.isEmpty()) {
            DisplayUtils.showSnackMessage(getView().findViewById(R.id.contacts_linear_layout),
                                          R.string.contacts_preferences_no_file_found);
        } else {
            final User user = contactsPreferenceActivity.getUser().orElseThrow(RuntimeException::new);
            OCFile[] files = new OCFile[backupToRestore.size()];
            Fragment contactListFragment = BackupListFragment.newInstance(backupToRestore.toArray(files), user);

            contactsPreferenceActivity.getSupportFragmentManager().
                beginTransaction()
                .replace(R.id.frame_container, contactListFragment, BackupListFragment.TAG)
                .addToBackStack(ContactsPreferenceActivity.BACKUP_TO_LIST)
                .commit();
        }
    }
}
