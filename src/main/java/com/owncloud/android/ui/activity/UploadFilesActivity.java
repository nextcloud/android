/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.files.services.FileUploader;
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
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_ACCOUNT;

/**
 * Displays local files and let the user choose what of them wants to upload to the current ownCloud account.
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
    private Spinner mBehaviourSpinner;
    private MaterialButton uploadButton;

    /**
     * Helper to launch the UploadFilesActivity for which you would like a result when it finished. Your
     * onActivityResult() method will be called with the given requestCode.
     *
     * @param activity    the activity which should call the upload activity for a result
     * @param account     the account for which the upload activity is called
     * @param requestCode If >= 0, this code will be returned in onActivityResult()
     */
    public static void startUploadActivityForResult(Activity activity, Account account, int requestCode) {
        Intent action = new Intent(activity, UploadFilesActivity.class);
        action.putExtra(EXTRA_ACCOUNT, account);
        action.putExtra(REQUEST_CODE_KEY, requestCode);
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
        }

        if (savedInstanceState != null) {
            mCurrentDir = new File(savedInstanceState.getString(UploadFilesActivity.KEY_DIRECTORY_PATH,
                                                                Environment.getExternalStorageDirectory().getAbsolutePath()));
            mSelectAll = savedInstanceState.getBoolean(UploadFilesActivity.KEY_ALL_SELECTED, false);
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
        setContentView(R.layout.upload_files_layout);

        if (mLocalFolderPickerMode) {
            findViewById(R.id.upload_options).setVisibility(View.GONE);
            ((MaterialButton) findViewById(R.id.upload_files_btn_upload)).setText(R.string.uploader_btn_alternative_text);
        }

        mFileListFragment = (LocalFileListFragment) getSupportFragmentManager().findFragmentByTag("local_files_list");

        // Set input controllers
        MaterialButton cancelButton = findViewById(R.id.upload_files_btn_cancel);
        cancelButton.setTextColor(ThemeColorUtils.primaryColor(this, true));
        cancelButton.setOnClickListener(this);

        uploadButton = findViewById(R.id.upload_files_btn_upload);
        ThemeButtonUtils.colorPrimaryButton(uploadButton, this);
        uploadButton.setOnClickListener(this);
        uploadButton.setEnabled(false);

        int localBehaviour = preferences.getUploaderBehaviour();

        // file upload spinner
        mBehaviourSpinner = findViewById(R.id.upload_files_spinner_behaviour);

        List<String> behaviours = new ArrayList<>();
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_move_to_nextcloud_folder,
                                 ThemeUtils.getDefaultDisplayNameForRootFolder(this)));
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_only_upload));
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_upload_and_delete_from_source));

        ArrayAdapter<String> behaviourAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                                                                   behaviours);
        behaviourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBehaviourSpinner.setAdapter(behaviourAdapter);
        mBehaviourSpinner.setSelection(localBehaviour);

        // setup the toolbar
        setupToolbar();
        findViewById(R.id.sort_list_button_group).setVisibility(View.VISIBLE);
        findViewById(R.id.switch_grid_view_button).setVisibility(View.GONE);

        // Action bar setup
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);   // mandatory since Android ICS, according to the official documentation
            actionBar.setDisplayHomeAsUpEnabled(mCurrentDir != null);
            actionBar.setDisplayShowTitleEnabled(false);

            ThemeToolbarUtils.tintBackButton(actionBar, this);
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

        Log_OC.d(TAG, "onCreate() end");
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

        int fontColor = ThemeColorUtils.appBarPrimaryFontColor(this);
        final MenuItem item = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);
        ThemeToolbarUtils.themeSearchView(mSearchView, this);
        ThemeDrawableUtils.tintDrawable(menu.findItem(R.id.action_choose_storage_path).getIcon(), fontColor);

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
            item.setChecked(!item.isChecked());
            mSelectAll = item.isChecked();
            setSelectAllMenuItem(item, mSelectAll);
            mFileListFragment.selectAllFiles(item.isChecked());
        } else if (itemId == R.id.action_choose_storage_path) {
            showLocalStoragePathPickerDialog();
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private void showLocalStoragePathPickerDialog() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);
        dialog = LocalStoragePathPickerDialogFragment.newInstance();
        dialog.show(ft, LocalStoragePathPickerDialogFragment.LOCAL_STORAGE_PATH_PICKER_FRAGMENT);
    }

    @Override
    public void onSortingOrderChosen(FileSortOrder selection) {
        preferences.setSortOrder(FileSortOrder.Type.uploadFilesView, selection);
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

    @Override
    public void onBackPressed() {
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
                showLocalStoragePathPickerDialog();
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.d(TAG, "onSaveInstanceState() start");
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

    private void setSelectAllMenuItem(MenuItem selectAll, boolean checked) {
        selectAll.setChecked(checked);
        if (checked) {
            selectAll.setIcon(R.drawable.ic_select_none);
        } else {
            selectAll.setIcon(
                ThemeDrawableUtils.tintDrawable(R.drawable.ic_select_all, ThemeColorUtils.primaryColor(this)));
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
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }

        if (hasEnoughSpaceAvailable) {
            // return the list of files (success)
            Intent data = new Intent();

            if (requestCode == FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA) {
                data.putExtra(EXTRA_CHOSEN_FILES, new String[]{filesToUpload[0]});
                setResult(RESULT_OK_AND_DELETE, data);

                preferences.setUploaderBehaviour(FileUploader.LOCAL_BEHAVIOUR_DELETE);
            } else {
                data.putExtra(EXTRA_CHOSEN_FILES, mFileListFragment.getCheckedFilePaths());
                data.putExtra(LOCAL_BASE_PATH, mCurrentDir.getAbsolutePath());

                // set result code
                switch (mBehaviourSpinner.getSelectedItemPosition()) {
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
                preferences.setUploaderBehaviour(mBehaviourSpinner.getSelectedItemPosition());
            }

            finish();
        } else {
            // show a dialog to query the user if wants to move the selected files
            // to the ownCloud folder instead of copying
            String[] args = {getString(R.string.app_name)};
            ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                R.string.upload_query_move_foreign_files, args, 0, R.string.common_yes, -1,
                R.string.common_no
                                                                                      );
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
        mBehaviourSpinner.setEnabled(canWriteIntoFolder);

        TextView textView = findViewById(R.id.upload_files_upload_files_behaviour_text);

        if (canWriteIntoFolder) {
            textView.setText(getString(R.string.uploader_upload_files_behaviour));
            int localBehaviour = preferences.getUploaderBehaviour();
            mBehaviourSpinner.setSelection(localBehaviour);
        } else {
            mBehaviourSpinner.setSelection(1);
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
        uploadButton.setEnabled(mFileListFragment.getCheckedFilesCount() > 0);
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
                    .execute(mBehaviourSpinner.getSelectedItemPosition() == 0);
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
        if (getAccount() != null) {
            if (!mAccountOnCreation.equals(getAccount())) {
                setResult(RESULT_CANCELED);
                finish();
            }

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
