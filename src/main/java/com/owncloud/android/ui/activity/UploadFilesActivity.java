/**
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
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.LocalFileListFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;

import static com.owncloud.android.db.PreferenceManager.getSortAscending;
import static com.owncloud.android.db.PreferenceManager.getSortOrder;


/**
 * Displays local files and let the user choose what of them wants to upload
 * to the current ownCloud account.
 */
public class UploadFilesActivity extends FileActivity implements
    LocalFileListFragment.ContainerActivity, ActionBar.OnNavigationListener,
        OnClickListener, ConfirmationDialogFragmentListener, SortingOrderDialogFragment.OnSortingOrderListener {

    private static final String SORT_ORDER_DIALOG_TAG = "SORT_ORDER_DIALOG";

    private ArrayAdapter<String> mDirectories;
    private File mCurrentDir = null;
    private boolean mSelectAll = false;
    private boolean mLocalFolderPickerMode = false;
    private LocalFileListFragment mFileListFragment;
    private Button mCancelBtn;
    protected Button mUploadBtn;
    private Spinner mBehaviourSpinner;
    private Account mAccountOnCreation;
    private DialogFragment mCurrentDialog;
    private Menu mOptionsMenu;

    private static final String SCREEN_NAME = "Choose local files to upload";

    public static final String EXTRA_CHOSEN_FILES =
            UploadFilesActivity.class.getCanonicalName() + ".EXTRA_CHOSEN_FILES";

    public static final String EXTRA_ACTION = UploadFilesActivity.class.getCanonicalName() + ".EXTRA_ACTION";
    public final static String KEY_LOCAL_FOLDER_PICKER_MODE = UploadFilesActivity.class.getCanonicalName()
            + ".LOCAL_FOLDER_PICKER_MODE";

    public static final int RESULT_OK_AND_MOVE = RESULT_FIRST_USER;
    public static final int RESULT_OK_AND_DO_NOTHING = 2;
    public static final int RESULT_OK_AND_DELETE = 3;

    public static final String KEY_DIRECTORY_PATH =
            UploadFilesActivity.class.getCanonicalName() + ".KEY_DIRECTORY_PATH";
    private static final String KEY_ALL_SELECTED =
            UploadFilesActivity.class.getCanonicalName() + ".KEY_ALL_SELECTED";

    private static final String TAG = "UploadFilesActivity";
    private static final String WAIT_DIALOG_TAG = "WAIT";
    private static final String QUERY_TO_MOVE_DIALOG_TAG = "QUERY_TO_MOVE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mLocalFolderPickerMode = extras.getBoolean(KEY_LOCAL_FOLDER_PICKER_MODE, false);
        }

        if(savedInstanceState != null) {
            mCurrentDir = new File(savedInstanceState.getString(UploadFilesActivity.KEY_DIRECTORY_PATH, Environment
                    .getExternalStorageDirectory().getAbsolutePath()));
            mSelectAll = savedInstanceState.getBoolean(UploadFilesActivity.KEY_ALL_SELECTED, false);
        } else {
            mCurrentDir = Environment.getExternalStorageDirectory();
        }
        
        mAccountOnCreation = getAccount();
                
        /// USER INTERFACE
            
        // Drop-down navigation 
        mDirectories = new CustomArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item);
        File currDir = mCurrentDir;
        while(currDir != null && currDir.getParentFile() != null) {
            mDirectories.add(currDir.getName());
            currDir = currDir.getParentFile();
        }
        mDirectories.add(File.separator);

        // Inflate and set the layout view
        setContentView(R.layout.upload_files_layout);

        if (mLocalFolderPickerMode) {
            findViewById(R.id.upload_options).setVisibility(View.GONE);
            ((AppCompatButton) findViewById(R.id.upload_files_btn_upload))
                    .setText(R.string.uploader_btn_alternative_text);
        }

        mFileListFragment = (LocalFileListFragment) getSupportFragmentManager().findFragmentById(R.id.local_files_list);
        
        // Set input controllers
        mCancelBtn = (Button) findViewById(R.id.upload_files_btn_cancel);
        mCancelBtn.setOnClickListener(this);
        mUploadBtn = (AppCompatButton) findViewById(R.id.upload_files_btn_upload);
        mUploadBtn.getBackground().setColorFilter(ThemeUtils.primaryAccentColor(), PorterDuff.Mode.SRC_ATOP);
        mUploadBtn.setOnClickListener(this);

        int localBehaviour = PreferenceManager.getUploaderBehaviour(this);

        // file upload spinner
        mBehaviourSpinner = (Spinner) findViewById(R.id.upload_files_spinner_behaviour);
        ArrayAdapter<CharSequence> behaviourAdapter = ArrayAdapter.createFromResource(this,
                R.array.upload_files_behaviour, android.R.layout.simple_spinner_item);
        behaviourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBehaviourSpinner.setAdapter(behaviourAdapter);
        mBehaviourSpinner.setSelection(localBehaviour);

        // setup the toolbar
        setupToolbar();
            
        // Action bar setup
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);   // mandatory since Android ICS, according to the
                                                // official documentation
        actionBar.setDisplayHomeAsUpEnabled(mCurrentDir != null && mCurrentDir.getName() != null);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mDirectories, this);

        // wait dialog
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
            mCurrentDialog = null;
        }
            
        Log_OC.d(TAG, "onCreate() end");
    }

    /**
     * Helper to launch the UploadFilesActivity for which you would like a result when it finished.
     * Your onActivityResult() method will be called with the given requestCode.
     *
     * @param activity    the activity which should call the upload activity for a result
     * @param account     the account for which the upload activity is called
     * @param requestCode If >= 0, this code will be returned in onActivityResult()
     */
    public static void startUploadActivityForResult(Activity activity, Account account, int requestCode) {
        Intent action = new Intent(activity, UploadFilesActivity.class);
        action.putExtra(EXTRA_ACCOUNT, (account));
        activity.startActivityForResult(action, requestCode);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.upload_files_picker, menu);

        if(mLocalFolderPickerMode) {
            menu.removeItem(R.id.action_select_all);
            menu.removeItem(R.id.action_search);
        } else {
            MenuItem selectAll = menu.findItem(R.id.action_select_all);
            setSelectAllMenuItem(selectAll, mSelectAll);
        }

        MenuItem switchView = menu.findItem(R.id.action_switch_view);
        switchView.setTitle(isGridView() ? R.string.action_switch_list_view : R.string.action_switch_grid_view);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if(mCurrentDir != null && mCurrentDir.getParentFile() != null){
                    onBackPressed(); 
                }
                break;
            }
            case R.id.action_select_all: {
                item.setChecked(!item.isChecked());
                mSelectAll = item.isChecked();
                setSelectAllMenuItem(item, mSelectAll);
                mFileListFragment.selectAllFiles(item.isChecked());
                break;
            }
            case R.id.action_sort: {
                // Read sorting order, default to sort by name ascending
                Integer sortOrder = PreferenceManager.getSortOrder(this);

                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.addToBackStack(null);

                SortingOrderDialogFragment mSortingOrderDialogFragment = SortingOrderDialogFragment.newInstance(
                        getSortOrder(this),
                        getSortAscending(this)
                );
                mSortingOrderDialogFragment.show(ft, SORT_ORDER_DIALOG_TAG);

                break;
            }
            case R.id.action_switch_view: {
                if (isGridView()) {
                    item.setTitle(getString(R.string.action_switch_grid_view));
                    item.setIcon(R.drawable.ic_view_module);
                    mFileListFragment.switchToListView();
                } else {
                    item.setTitle(getApplicationContext().getString(R.string.action_switch_list_view));
                    item.setIcon(R.drawable.ic_view_list);
                    mFileListFragment.switchToGridView();
                }
            }
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    @Override
    public void onSortingOrderChosen(int selection) {
        switch (selection) {
            case SortingOrderDialogFragment.BY_NAME_ASC:
                mFileListFragment.sortByName(true);
                break;
            case SortingOrderDialogFragment.BY_NAME_DESC:
                mFileListFragment.sortByName(false);
                break;
            case SortingOrderDialogFragment.BY_MODIFICATION_DATE_ASC:
                mFileListFragment.sortByDate(true);
                break;
            case SortingOrderDialogFragment.BY_MODIFICATION_DATE_DESC:
                mFileListFragment.sortByDate(false);
                break;
            case SortingOrderDialogFragment.BY_SIZE_ASC:
                mFileListFragment.sortBySize(true);
                break;
            case SortingOrderDialogFragment.BY_SIZE_DESC:
                mFileListFragment.sortBySize(false);
                break;
            default: // defaulting to alphabetical-ascending
                Log_OC.w(TAG, "Unknown sort order, defaulting to alphabetical-ascending!");
                mFileListFragment.sortByName(true);
                break;
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int i = itemPosition;
        while (i-- != 0) {
            onBackPressed();
        }
        // the next operation triggers a new call to this method, but it's necessary to 
        // ensure that the name exposed in the action bar is the current directory when the 
        // user selected it in the navigation list
        if (itemPosition != 0) {
            getSupportActionBar().setSelectedNavigationItem(0);
        }
        return true;
    }

    
    @Override
    public void onBackPressed() {
        if (mDirectories.getCount() <= 1) {
            finish();
            return;
        }
        popDirname();
        mFileListFragment.onNavigateUp();
        mCurrentDir = mFileListFragment.getCurrentDirectory();
        
        if(mCurrentDir.getParentFile() == null){
            ActionBar actionBar = getSupportActionBar(); 
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        // invalidate checked state when navigating directories
        if(!mLocalFolderPickerMode) {
            setSelectAllMenuItem(mOptionsMenu.findItem(R.id.action_select_all), false);
        }
    }

    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.d(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putString(UploadFilesActivity.KEY_DIRECTORY_PATH, mCurrentDir.getAbsolutePath());
        outState.putBoolean(UploadFilesActivity.KEY_ALL_SELECTED,
                mOptionsMenu.findItem(R.id.action_select_all).isChecked());
        Log_OC.d(TAG, "onSaveInstanceState() end");
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    /**
     * Pushes a directory to the drop down list
     * @param directory to push
     * @throws IllegalArgumentException If the {@link File#isDirectory()} returns false.
     */
    public void pushDirname(File directory) {
        if(!directory.isDirectory()){
            throw new IllegalArgumentException("Only directories may be pushed!");
        }
        mDirectories.insert(directory.getName(), 0);
        mCurrentDir = directory;
    }

    /**
     * Pops a directory name from the drop down list
     * @return True, unless the stack is empty
     */
    public boolean popDirname() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    private void setSelectAllMenuItem(MenuItem selectAll, boolean checked) {
        selectAll.setChecked(checked);
        if(checked) {
            selectAll.setIcon(R.drawable.ic_select_none);
        } else {
            selectAll.setIcon(ThemeUtils.tintDrawable(R.drawable.ic_select_all, ThemeUtils.primaryColor()));
        }
    }


    // Custom array adapter to override text colors
    private class CustomArrayAdapter<T> extends ArrayAdapter<T> {
    
        public CustomArrayAdapter(UploadFilesActivity ctx, int view) {
            super(ctx, view);
        }
    
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
    
            ((TextView) v).setTextColor(getResources().getColorStateList(
                    android.R.color.white));
            return v;
        }
    
        public View getDropDownView(int position, View convertView,
                ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);
    
            ((TextView) v).setTextColor(getResources().getColorStateList(
                    android.R.color.white));
    
            return v;
        }
    
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDirectoryClick(File directory) {
        if(!mLocalFolderPickerMode) {
            // invalidate checked state when navigating directories
            MenuItem selectAll = mOptionsMenu.findItem(R.id.action_select_all);
            setSelectAllMenuItem(selectAll, false);
        }

        pushDirname(directory);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileClick(File file) {
        // nothing to do
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
     * 
     * TODO Make here the real request to the Upload service ; will require to receive the account and 
     * target folder where the upload must be done in the received intent.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.upload_files_btn_cancel) {
            setResult(RESULT_CANCELED);
            finish();

        } else if (v.getId() == R.id.upload_files_btn_upload) {
            if(mLocalFolderPickerMode) {
                Intent data = new Intent();
                if(mCurrentDir != null) {
                    data.putExtra(EXTRA_CHOSEN_FILES, mCurrentDir.getAbsolutePath());
                }
                setResult(RESULT_OK, data);

                finish();
            } else {
                new CheckAvailableSpaceTask().execute(mBehaviourSpinner.getSelectedItemPosition() == 0);
            }
        }
    }

    /**
     * Asynchronous task checking if there is space enough to copy all the files chosen
     * to upload into the ownCloud local folder.
     * 
     * Maybe an AsyncTask is not strictly necessary, but who really knows.
     */
    private class CheckAvailableSpaceTask extends AsyncTask<Boolean, Void, Boolean> {

        /**
         * Updates the UI before trying the movement
         */
        @Override
        protected void onPreExecute () {
            /// progress dialog and disable 'Move' button
            mCurrentDialog = IndeterminateProgressDialog.newInstance(R.string.wait_a_moment, false);
            mCurrentDialog.show(getSupportFragmentManager(), WAIT_DIALOG_TAG);
        }

        /**
         * Checks the available space.
         *
         * @param params boolean flag if storage calculation should be done.
         * @return 'True' if there is space enough or doesn't have to be calculated
         */
        @Override
        protected Boolean doInBackground(Boolean... params) {
            if(params[0]) {
                String[] checkedFilePaths = mFileListFragment.getCheckedFilePaths();
                long total = 0;
                for (int i = 0; checkedFilePaths != null && i < checkedFilePaths.length; i++) {
                    String localPath = checkedFilePaths[i];
                    File localFile = new File(localPath);
                    total += localFile.length();
                }
                return FileStorageUtils.getUsableSpace(mAccountOnCreation.name) >= total;
            }

            return true;
        }

        /**
         * Updates the activity UI after the check of space is done.
         *
         * If there is not space enough. shows a new dialog to query the user if wants to move the
         * files instead of copy them.
         *
         * @param result        'True' when there is space enough to copy all the selected files.
         */
        @Override
        protected void onPostExecute(Boolean result) {
            if(mCurrentDialog != null) {
                mCurrentDialog.dismiss();
                mCurrentDialog = null;
            }
            
            if (result) {
                // return the list of selected files (success)
                Intent data = new Intent();
                data.putExtra(EXTRA_CHOSEN_FILES, mFileListFragment.getCheckedFilePaths());

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
                }

                // store behaviour
                PreferenceManager.setUploaderBehaviour(getApplicationContext(),
                        mBehaviourSpinner.getSelectedItemPosition());

                finish();
            } else {
                // show a dialog to query the user if wants to move the selected files
                // to the ownCloud folder instead of copying
                String[] args = {getString(R.string.app_name)};
                ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                    R.string.upload_query_move_foreign_files, args, 0, R.string.common_yes, -1,
                        R.string.common_no
                );
                dialog.setOnConfirmationListener(UploadFilesActivity.this);
                dialog.show(getSupportFragmentManager(), QUERY_TO_MOVE_DIALOG_TAG);
            }
        }
    }

    @Override
    public void onConfirmation(String callerTag) {
        Log_OC.d(TAG, "Positive button in dialog was clicked; dialog tag is " + callerTag);
        if (callerTag.equals(QUERY_TO_MOVE_DIALOG_TAG)) {
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
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
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
        Fragment listOfFiles = mFileListFragment;
        if (listOfFiles != null) {
            return (ExtendedListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }
}
