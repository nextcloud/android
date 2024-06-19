/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.utils.extensions.ActivityExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UploadFilesLayoutBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.StoragePathAdapter;
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.LocalFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.PermissionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_USER;

/**
 * Displays local files and let the user choose what of them wants to upload to the current Nextcloud account.
 */
public class UploadFilesActivity extends DrawerActivity implements LocalFileListFragment.ContainerActivity,
    OnClickListener, ConfirmationDialogFragmentListener, SortingOrderDialogFragment.OnSortingOrderListener,
    CheckAvailableSpaceTask.CheckAvailableSpaceListener, StoragePathAdapter.StoragePathAdapterListener, Injectable {

    private static final String KEY_ALL_SELECTED = UploadFilesActivity.class.getCanonicalName() + ".KEY_ALL_SELECTED";
    public final static String KEY_LOCAL_FOLDER_PICKER_MODE = UploadFilesActivity.class.getCanonicalName() + ".LOCAL_FOLDER_PICKER_MODE";
    public static final String LOCAL_BASE_PATH = UploadFilesActivity.class.getCanonicalName() + ".LOCAL_BASE_PATH";
    public static final String EXTRA_CHOSEN_FILES = UploadFilesActivity.class.getCanonicalName() + ".EXTRA_CHOSEN_FILES";
    public static final String KEY_DIRECTORY_PATH = UploadFilesActivity.class.getCanonicalName() + ".KEY_DIRECTORY_PATH";

    private static final int SINGLE_DIR = 1;
    public static final int RESULT_OK_AND_DELETE = 3;
    public static final int RESULT_OK_AND_DO_NOTHING = 2;
    public static final int RESULT_OK_AND_MOVE = RESULT_FIRST_USER;
    public static final String REQUEST_CODE_KEY = "requestCode";
    private static final String ENCRYPTED_FOLDER_KEY = "encrypted_folder";

    private static final String QUERY_TO_MOVE_DIALOG_TAG = "QUERY_TO_MOVE";
    private static final String TAG = "UploadFilesActivity";
    private static final String WAIT_DIALOG_TAG = "WAIT";

    @Inject AppPreferences preferences;
    private Account mAccountOnCreation;
    private ArrayAdapter<String> mDirectories;
    private boolean mLocalFolderPickerMode;
    private boolean mSelectAll;
    private DialogFragment mCurrentDialog;
    private File mCurrentDir;
    private int requestCode;
    private LocalFileListFragment mFileListFragment;
    private LocalStoragePathPickerDialogFragment dialog;
    private Menu mOptionsMenu;
    private SearchView mSearchView;
    private UploadFilesLayoutBinding binding;
    private boolean isWithinEncryptedFolder = false;


    @VisibleForTesting
    public LocalFileListFragment getFileListFragment() {
        return mFileListFragment;
    }

    /**
     * Helper to launch the UploadFilesActivity for which you would like a result when it finished. Your
     * onActivityResult() method will be called with the given requestCode.
     *
     * @param activity    the activity which should call the upload activity for a result
     * @param user        the user for which the upload activity is called
     * @param requestCode If >= 0, this code will be returned in onActivityResult()
     */
    public static void startUploadActivityForResult(Activity activity,
                                                    User user,
                                                    int requestCode,
                                                    boolean isWithinEncryptedFolder) {
        Intent action = new Intent(activity, UploadFilesActivity.class);
        action.putExtra(EXTRA_USER, user);
        action.putExtra(REQUEST_CODE_KEY, requestCode);
        action.putExtra(ENCRYPTED_FOLDER_KEY, isWithinEncryptedFolder);
        activity.startActivityForResult(action, requestCode);
    }

    @Override
    @SuppressLint("WrongViewCast") // wrong error on finding local_files_list
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mLocalFolderPickerMode = extras.getBoolean(KEY_LOCAL_FOLDER_PICKER_MODE, false);
            requestCode = (int) extras.get(REQUEST_CODE_KEY);
            isWithinEncryptedFolder = extras.getBoolean(ENCRYPTED_FOLDER_KEY, false);
        }

        if (savedInstanceState != null) {
            mCurrentDir = new File(savedInstanceState.getString(KEY_DIRECTORY_PATH,
                                                                Environment.getExternalStorageDirectory().getAbsolutePath()));
            mSelectAll = savedInstanceState.getBoolean(KEY_ALL_SELECTED, false);
            isWithinEncryptedFolder = savedInstanceState.getBoolean(ENCRYPTED_FOLDER_KEY, false);
        } else {
            String lastUploadFrom = preferences.getUploadFromLocalLastPath();

            if (!lastUploadFrom.isEmpty()) {
                mCurrentDir = new File(lastUploadFrom);

                while (!mCurrentDir.exists()) {
                    mCurrentDir = mCurrentDir.getParentFile();
                }
            } else {
                mCurrentDir = Environment.getExternalStorageDirectory();
            }
        }

        mAccountOnCreation = getAccount();

        /// USER INTERFACE

        // Drop-down navigation
        mDirectories = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        mDirectories.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fillDirectoryDropdown();

        // Inflate and set the layout view
        binding = UploadFilesLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (mLocalFolderPickerMode) {
            binding.uploadOptions.setVisibility(View.GONE);
            binding.uploadFilesBtnUpload.setText(R.string.uploader_btn_alternative_text);
        }

        mFileListFragment = (LocalFileListFragment) getSupportFragmentManager().findFragmentByTag("local_files_list");

        // Set input controllers
        viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.uploadFilesBtnCancel);
        binding.uploadFilesBtnCancel.setOnClickListener(this);

        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.uploadFilesBtnUpload);
        binding.uploadFilesBtnUpload.setOnClickListener(this);
        binding.uploadFilesBtnUpload.setEnabled(mLocalFolderPickerMode);

        int localBehaviour = preferences.getUploaderBehaviour();

        // file upload spinner
        List<String> behaviours = new ArrayList<>();
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_move_to_nextcloud_folder,
                                 themeUtils.getDefaultDisplayNameForRootFolder(this)));
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_only_upload));
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_upload_and_delete_from_source));

        ArrayAdapter<String> behaviourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                                                                   behaviours);
        behaviourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.uploadFilesSpinnerBehaviour.setAdapter(behaviourAdapter);
        binding.uploadFilesSpinnerBehaviour.setSelection(localBehaviour);

        // setup the toolbar
        setupToolbar();
        binding.uploadFilesToolbar.sortListButtonGroup.setVisibility(View.VISIBLE);
        binding.uploadFilesToolbar.switchGridViewButton.setVisibility(View.GONE);

        // Action bar setup
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);   // mandatory since Android ICS, according to the official documentation
            actionBar.setDisplayHomeAsUpEnabled(mCurrentDir != null);
            actionBar.setDisplayShowTitleEnabled(false);

            viewThemeUtils.files.themeActionBar(this, actionBar);
        }

        showToolbarSpinner();
        mToolbarSpinner.setAdapter(mDirectories);
        mToolbarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int i = position;
                while (i-- != 0) {
                    onBackPressed();
                }
                // the next operation triggers a new call to this method, but it's necessary to
                // ensure that the name exposed in the action bar is the current directory when the
                // user selected it in the navigation list
                if (position != 0) {
                    mToolbarSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no action
            }
        });

        // wait dialog
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        checkWritableFolder(mCurrentDir);

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        Log_OC.d(TAG, "onCreate() end");
    }

    private void requestPermissions() {
        PermissionUtil.requestExternalStoragePermission(this, viewThemeUtils, true);
    }

    public void showToolbarSpinner() {
        mToolbarSpinner.setVisibility(View.VISIBLE);
    }

    private void fillDirectoryDropdown() {
        File currentDir = mCurrentDir;
        while (currentDir != null && currentDir.getParentFile() != null) {
            mDirectories.add(currentDir.getName());
            currentDir = currentDir.getParentFile();
        }
        mDirectories.add(File.separator);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.activity_upload_files, menu);

        if (!mLocalFolderPickerMode) {
            MenuItem selectAll = menu.findItem(R.id.action_select_all);
            setSelectAllMenuItem(selectAll, mSelectAll);
        }

        final MenuItem item = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);
        viewThemeUtils.androidx.themeToolbarSearchView(mSearchView);
        viewThemeUtils.platform.tintTextDrawable(this, menu.findItem(R.id.action_choose_storage_path).getIcon());

        mSearchView.setOnSearchClickListener(v -> mToolbarSpinner.setVisibility(View.GONE));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            if (mCurrentDir != null && mCurrentDir.getParentFile() != null) {
                onBackPressed();
            }
        } else if (itemId == R.id.action_select_all) {
            mSelectAll = !item.isChecked();
            item.setChecked(mSelectAll);
            mFileListFragment.selectAllFiles(mSelectAll);
            setSelectAllMenuItem(item, mSelectAll);
        } else if (itemId == R.id.action_choose_storage_path) {
            checkLocalStoragePathPickerPermission();
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private void checkLocalStoragePathPickerPermission() {
        if (!PermissionUtil.checkExternalStoragePermission(this)) {
            requestPermissions();
        } else {
            showLocalStoragePathPickerDialog();
        }
    }

    private void showLocalStoragePathPickerDialog() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog = LocalStoragePathPickerDialogFragment.newInstance();
        dialog.show(ft, LocalStoragePathPickerDialogFragment.LOCAL_STORAGE_PATH_PICKER_FRAGMENT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                showLocalStoragePathPickerDialog();
            } else {
                DisplayUtils.showSnackMessage(this, R.string.permission_storage_access);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onSortingOrderChosen(FileSortOrder selection) {
        preferences.setSortOrder(FileSortOrder.Type.localFileListView, selection);
        mFileListFragment.sortFiles(selection);
    }

    private boolean isSearchOpen() {
        if (mSearchView == null) {
            return false;
        } else {
            View mSearchEditFrame = mSearchView.findViewById(androidx.appcompat.R.id.search_edit_frame);
            return mSearchEditFrame != null && mSearchEditFrame.getVisibility() == View.VISIBLE;
        }
    }

    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (isSearchOpen() && mSearchView != null) {
                mSearchView.setQuery("", false);
                mFileListFragment.onClose();
                mSearchView.onActionViewCollapsed();
                setDrawerIndicatorEnabled(isDrawerIndicatorAvailable());
            } else {
                if (mDirectories.getCount() <= SINGLE_DIR) {
                    finish();
                    return;
                }

                File parentFolder = mCurrentDir.getParentFile();
                if (!parentFolder.canRead()) {
                    checkLocalStoragePathPickerPermission();
                    return;
                }

                popDirname();
                mFileListFragment.onNavigateUp();
                mCurrentDir = mFileListFragment.getCurrentDirectory();
                checkWritableFolder(mCurrentDir);

                if (mCurrentDir.getParentFile() == null) {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setDisplayHomeAsUpEnabled(false);
                    }
                }

                // invalidate checked state when navigating directories
                if (!mLocalFolderPickerMode) {
                    setSelectAllMenuItem(mOptionsMenu.findItem(R.id.action_select_all), false);
                }
            }
        }
    };

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        FileExtensionsKt.logFileSize(mCurrentDir, TAG);
        super.onSaveInstanceState(outState);
        outState.putString(UploadFilesActivity.KEY_DIRECTORY_PATH, mCurrentDir.getAbsolutePath());
        if (mOptionsMenu != null && mOptionsMenu.findItem(R.id.action_select_all) != null) {
            outState.putBoolean(UploadFilesActivity.KEY_ALL_SELECTED, mOptionsMenu.findItem(R.id.action_select_all).isChecked());
        } else {
            outState.putBoolean(UploadFilesActivity.KEY_ALL_SELECTED, false);
        }
        Log_OC.d(TAG, "onSaveInstanceState() end");
    }

    /**
     * Pushes a directory to the drop down list
     *
     * @param directory to push
     * @throws IllegalArgumentException If the {@link File#isDirectory()} returns false.
     */
    public void pushDirname(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Only directories may be pushed!");
        }
        mDirectories.insert(directory.getName(), 0);
        mCurrentDir = directory;
        checkWritableFolder(mCurrentDir);
    }

    /**
     * Pops a directory name from the drop down list
     *
     * @return True, unless the stack is empty
     */
    public boolean popDirname() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    private void updateUploadButtonActive() {
        final boolean anySelected = mFileListFragment.getCheckedFilesCount() > 0;
        binding.uploadFilesBtnUpload.setEnabled(anySelected || mLocalFolderPickerMode);
    }

    private void setSelectAllMenuItem(MenuItem selectAll, boolean checked) {
        if (selectAll != null) {
            selectAll.setChecked(checked);
            if (checked) {
                selectAll.setIcon(R.drawable.ic_select_none);
            } else {
                selectAll.setIcon(
                    viewThemeUtils.platform.tintPrimaryDrawable(this, R.drawable.ic_select_all));
            }
            updateUploadButtonActive();
        }
    }

    @Override
    public void onCheckAvailableSpaceStart() {
        if (requestCode == FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM) {
            mCurrentDialog = IndeterminateProgressDialog.newInstance(R.string.wait_a_moment, false);
            mCurrentDialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);
        }
    }

    /**
     * Updates the activity UI after the check of space is done. If there is not space enough. shows a new dialog to
     * query the user if wants to move the files instead of copy them.
     *
     * @param hasEnoughSpaceAvailable 'True' when there is space enough to copy all the selected files.
     */
    @Override
    public void onCheckAvailableSpaceFinish(boolean hasEnoughSpaceAvailable, String... filesToUpload) {
        if (mCurrentDialog != null && ActivityExtensionsKt.isDialogFragmentReady(this, mCurrentDialog)) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        if (hasEnoughSpaceAvailable) {
            // return the list of files (success)
            Intent data = new Intent();

            if (requestCode == FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA) {
                data.putExtra(EXTRA_CHOSEN_FILES, new String[]{filesToUpload[0]});
                setResult(RESULT_OK_AND_DELETE, data);

                preferences.setUploaderBehaviour(FileUploadWorker.LOCAL_BEHAVIOUR_DELETE);
            } else {
                data.putExtra(EXTRA_CHOSEN_FILES, mFileListFragment.getCheckedFilePaths());
                data.putExtra(LOCAL_BASE_PATH, mCurrentDir.getAbsolutePath());

                // set result code
                switch (binding.uploadFilesSpinnerBehaviour.getSelectedItemPosition()) {
                    case 0: // move to nextcloud folder
                        setResult(RESULT_OK_AND_MOVE, data);
                        break;

                    case 1: // only upload
                        setResult(RESULT_OK_AND_DO_NOTHING, data);
                        break;

                    case 2: // upload and delete from source
                        setResult(RESULT_OK_AND_DELETE, data);
                        break;

                    default:
                        // do nothing
                        break;
                }

                // store behaviour
                preferences.setUploaderBehaviour(binding.uploadFilesSpinnerBehaviour.getSelectedItemPosition());
            }

            finish();
        } else {
            // show a dialog to query the user if wants to move the selected files
            // to the ownCloud folder instead of copying
            String[] args = { getString(R.string.app_name) };
            ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                R.string.upload_query_move_foreign_files, args, 0, R.string.common_yes,  R.string.common_no, -1);
            dialog.setOnConfirmationListener(this);
            dialog.show(getSupportFragmentManager(), QUERY_TO_MOVE_DIALOG_TAG);
        }
    }

    @Override
    public void chosenPath(String path) {
        if (getListOfFilesFragment() instanceof LocalFileListFragment) {
            File file = new File(path);
            ((LocalFileListFragment) getListOfFilesFragment()).listDirectory(file);
            onDirectoryClick(file);

            mCurrentDir = new File(path);
            mDirectories.clear();

            fillDirectoryDropdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDirectoryClick(File directory) {
        if (!mLocalFolderPickerMode) {
            // invalidate checked state when navigating directories
            MenuItem selectAll = mOptionsMenu.findItem(R.id.action_select_all);
            setSelectAllMenuItem(selectAll, false);
        }

        pushDirname(directory);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void checkWritableFolder(File folder) {
        boolean canWriteIntoFolder = folder.canWrite();
        binding.uploadFilesSpinnerBehaviour.setEnabled(canWriteIntoFolder);

        TextView textView = findViewById(R.id.upload_files_upload_files_behaviour_text);

        if (canWriteIntoFolder) {
            textView.setText(getString(R.string.uploader_upload_files_behaviour));
            int localBehaviour = preferences.getUploaderBehaviour();
            binding.uploadFilesSpinnerBehaviour.setSelection(localBehaviour);
        } else {
            binding.uploadFilesSpinnerBehaviour.setSelection(1);
            textView.setText(new StringBuilder().append(getString(R.string.uploader_upload_files_behaviour))
                                 .append(' ')
                                 .append(getString(R.string.uploader_upload_files_behaviour_not_writable))
                                 .toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileClick(File file) {
        updateUploadButtonActive();

        boolean selectAll = mFileListFragment.getCheckedFilesCount() == mFileListFragment.getFilesCount();
        setSelectAllMenuItem(mOptionsMenu.findItem(R.id.action_select_all), selectAll);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getInitialDirectory() {
        return mCurrentDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFolderPickerMode() {
        return mLocalFolderPickerMode;
    }

    @Override
    public boolean isWithinEncryptedFolder() {
        return isWithinEncryptedFolder;
    }

    /**
     * Performs corresponding action when user presses 'Cancel' or 'Upload' button
     * <p>
     * TODO Make here the real request to the Upload service ; will require to receive the account and target folder
     * where the upload must be done in the received intent.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.upload_files_btn_cancel) {
            setResult(RESULT_CANCELED);
            finish();

        } else if (v.getId() == R.id.upload_files_btn_upload) {
            if (PermissionUtil.checkExternalStoragePermission(this)) {
                if (mCurrentDir != null) {
                    preferences.setUploadFromLocalLastPath(mCurrentDir.getAbsolutePath());
                }
                if (mLocalFolderPickerMode) {
                    Intent data = new Intent();
                    if (mCurrentDir != null) {
                        data.putExtra(EXTRA_CHOSEN_FILES, mCurrentDir.getAbsolutePath());
                    }
                    setResult(RESULT_OK, data);

                    finish();
                } else {
                    new CheckAvailableSpaceTask(this, mFileListFragment.getCheckedFilePaths())
                        .execute(binding.uploadFilesSpinnerBehaviour.getSelectedItemPosition() == 0);
                }
            } else {
                requestPermissions();
            }
        }
    }

    @Override
    public void onConfirmation(String callerTag) {
        Log_OC.d(TAG, "Positive button in dialog was clicked; dialog tag is " + callerTag);
        if (QUERY_TO_MOVE_DIALOG_TAG.equals(callerTag)) {
            // return the list of selected files to the caller activity (success),
            // signaling that they should be moved to the ownCloud folder, instead of copied
            Intent data = new Intent();
            data.putExtra(EXTRA_CHOSEN_FILES, mFileListFragment.getCheckedFilePaths());
            data.putExtra(LOCAL_BASE_PATH, mCurrentDir.getAbsolutePath());
            setResult(RESULT_OK_AND_MOVE, data);
            finish();
        }
    }

    @Override
    public void onNeutral(String callerTag) {
        Log_OC.d(TAG, "Phantom neutral button in dialog was clicked; dialog tag is " + callerTag);
    }

    @Override
    public void onCancel(String callerTag) {
        /// nothing to do; don't finish, let the user change the selection
        Log_OC.d(TAG, "Negative button in dialog was clicked; dialog tag is " + callerTag);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Account account = getAccount();
        if (mAccountOnCreation != null && mAccountOnCreation.equals(account)) {
            requestPermissions();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private boolean isGridView() {
        return getListOfFilesFragment().isGridEnabled();
    }

    private ExtendedListFragment getListOfFilesFragment() {
        if (mFileListFragment == null) {
            Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        }

        return mFileListFragment;
    }

    @Override
    protected void onStop() {
        if (dialog != null) {
            dialog.dismissAllowingStateLoss();
        }

        super.onStop();
    }
}
