/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
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
package com.owncloud.android.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.PopupMenu;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.FileActionsDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.ui.preview.PreviewTextFragment;

import java.io.File;
import java.util.ArrayList;

/**
 * A Fragment that lists all files and folders in a given path.
 *
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment {
    
    private static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_JUST_FOLDERS = MY_PACKAGE + ".JUST_FOLDERS";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";

    private FileFragment.ContainerActivity mContainerActivity;

    private OCFile mFile = null;
    private FileListListAdapter mAdapter;
    private boolean mJustFolders;

    private int mStatusBarColorActionMode;
    private int mStatusBarColor;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStatusBarColorActionMode = getResources().getColor(R.color.actionModeStatusBarBackground);
        mStatusBarColor = getResources().getColor(R.color.primary_dark);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log_OC.e(TAG, "onAttach");
        try {
            mContainerActivity = (FileFragment.ContainerActivity) activity;

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " +
                    FileFragment.ContainerActivity.class.getSimpleName());
        }
        try {
            setOnRefreshListener((OnEnforceableRefreshListener) activity);
            
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " +
                    SwipeRefreshLayout.OnRefreshListener.class.getSimpleName());
        }
    }


    @Override
    public void onDetach() {
        setOnRefreshListener(null);
        mContainerActivity = null;
        super.onDetach();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.e(TAG, "onActivityCreated() start");

        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(KEY_FILE);
        }

        if (mJustFolders) {
            setFooterEnabled(false);
        } else {
            setFooterEnabled(true);
        }

        Bundle args = getArguments();
        mJustFolders = (args == null) ? false : args.getBoolean(ARG_JUST_FOLDERS, false);
        mAdapter = new FileListListAdapter(
                mJustFolders,
                getActivity(),
                mContainerActivity
        );
        setListAdapter(mAdapter);

        registerLongClickListener();
  }

    private void registerLongClickListener() {
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            private Menu menu;

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                final int checkedCount = getListView().getCheckedItemCount();
                // TODO Tobi extract to values
                mode.setTitle(checkedCount + " selected");

                if (checked) {
                    mAdapter.setNewSelection(position, checked);
                } else {
                    mAdapter.removeSelection(position);
                }

                // TODO maybe change: only recreate menu if count changes
                if (menu != null) {
                    menu.clear();
                    if (checkedCount == 1) {
                        createContextMenu(menu);
                    } else {
                        // download, move, copy, delete
                        getActivity().getMenuInflater().inflate(R.menu.multiple_file_actions_menu, menu);
                    }
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                this.menu = menu;

                //set gray color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().getWindow().setStatusBarColor(mStatusBarColorActionMode);
                }

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return onFileActionChosen(item.getItemId());
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.removeSelection();

                // reset to primary dark color
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    getActivity().getWindow().setStatusBarColor(mStatusBarColor);
                }
            }
        });
    }

    // TODO Tobi needed?
    private void showFileAction(int fileIndex) {
        Bundle args = getArguments();
        PopupMenu pm = new PopupMenu(getActivity(),null);
        Menu menu = pm.getMenu();

        boolean allowContextualActions =
                (args == null) ? true : args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, true);

        if (allowContextualActions) {
            MenuInflater inflater = getActivity().getMenuInflater();

            inflater.inflate(R.menu.file_actions_menu, menu);
            OCFile targetFile = (OCFile) mAdapter.getItem(fileIndex);

            if (mContainerActivity.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                        targetFile,
                        mContainerActivity.getStorageManager().getAccount(),
                        mContainerActivity,
                        getActivity()
                );
                mf.filter(menu);
            }

            /// TODO break this direct dependency on FileDisplayActivity... if possible
            MenuItem item = menu.findItem(R.id.action_open_file_with);
            FileFragment frag = ((FileDisplayActivity)getActivity()).getSecondFragment();
            if (frag != null && frag instanceof FileDetailFragment &&
                    frag.getFile().getFileId() == targetFile.getFileId()) {
                item = menu.findItem(R.id.action_see_details);
                if (item != null) {
                    item.setVisible(false);
                    item.setEnabled(false);
                }
            }

            FileActionsDialogFragment dialog = FileActionsDialogFragment.newInstance(menu, fileIndex, targetFile.getFileName());
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), FileActionsDialogFragment.FTAG_FILE_ACTIONS);
        }
    }

    /**
     * Saves the current listed folder.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILE, mFile);
    }

    /**
     * Call this, when the user presses the up button.
     *
     * Tries to move up the current folder one level. If the parent folder was removed from the
     * database, it continues browsing up until finding an existing folders.
     * <p/>
     * return       Count of folder levels browsed up.
     */
    public int onBrowseUp() {
        OCFile parentDir = null;
        int moveCount = 0;

        if (mFile != null) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
            }
            while (parentDir == null) {
                parentPath = new File(parentPath).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                        parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            }   // exit is granted because storageManager.getFileByPath("/") never returns null
            mFile = parentDir;

            // TODO Enable when "On Device" is recovered ?
            listDirectory(mFile /*, MainApp.getOnlyOnDevice()*/);

            onRefresh(false);

            // restore index and top position
            restoreIndexAndTopPosition();

        }   // else - should never happen now

        return moveCount;
    }

    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        OCFile file = (OCFile) mAdapter.getItem(position);
        if (file != null) {
            if (file.isFolder()) {
                // update state and view of this fragment
                // TODO Enable when "On Device" is recovered ?
                listDirectory(file/*, MainApp.getOnlyOnDevice()*/);
                // then, notify parent activity to let it update its state and view
                mContainerActivity.onBrowsedDownTo(file);
                // save index and top position
                saveIndexAndTopPosition(position);

            } else { /// Click on a file
                if (PreviewImageFragment.canBePreviewed(file)) {
                    // preview image - it handles the download, if needed
                    ((FileDisplayActivity)mContainerActivity).startImagePreview(file);
                } else if (PreviewTextFragment.canBePreviewed(file)){
                    ((FileDisplayActivity)mContainerActivity).startTextPreview(file);
                } else if (file.isDown()) {
                    if (PreviewMediaFragment.canBePreviewed(file)) {
                        // media preview
                        ((FileDisplayActivity) mContainerActivity).startMediaPreview(file, 0, true);
                    } else {
                        mContainerActivity.getFileOperationsHelper().openFile(file);
                    }

                } else {
                    // automatic download, preview on finish
                    ((FileDisplayActivity) mContainerActivity).startDownloadForPreview(file);
                }

            }

        } else {
            Log_OC.d(TAG, "Null object in ListAdapter!!");
        }

    }

    /**
     * {@inheritDoc}
     */
    // TODO Tobi needed?
    public void createContextMenu(Menu menu) {
        Bundle args = getArguments();
        boolean allowContextualActions =
                (args == null) ? true : args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        if (allowContextualActions) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            OCFile targetFile = null;
            if (mAdapter.getCheckedItems().size() == 1){
                targetFile = mAdapter.getCheckedItems().get(0);
            }

            if (mContainerActivity.getStorageManager() != null) {
                FileMenuFilter mf = new FileMenuFilter(
                    targetFile,
                    mContainerActivity.getStorageManager().getAccount(),
                    mContainerActivity,
                    getActivity()
                );
                mf.filter(menu);
            }
                 
            /// TODO break this direct dependency on FileDisplayActivity... if possible
            MenuItem item = menu.findItem(R.id.action_open_file_with);
            FileFragment frag = ((FileDisplayActivity)getActivity()).getSecondFragment();
            if (frag != null && frag instanceof FileDetailFragment && 
                    frag.getFile().getFileId() == targetFile.getFileId()) {
                item = menu.findItem(R.id.action_see_details);
                if (item != null) {
                    item.setVisible(false);
                    item.setEnabled(false);
                }
            }
        }
    }

    public boolean onFileActionChosen(int menuId) {
        if (mAdapter.getCheckedItems().size() == 1){
            OCFile mTargetFile = mAdapter.getCheckedItems().get(0);

            switch (menuId) {
                case R.id.action_share_file: {
                    mContainerActivity.getFileOperationsHelper().shareFileWithLink(mTargetFile);
                    return true;
                }
                case R.id.action_open_file_with: {
                    mContainerActivity.getFileOperationsHelper().openFile(mTargetFile);
                    return true;
                }
                case R.id.action_unshare_file: {
                    mContainerActivity.getFileOperationsHelper().unshareFileWithLink(mTargetFile);
                    return true;
                }
                case R.id.action_rename_file: {
                    RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(mTargetFile);
                    dialog.show(getFragmentManager(), FileDetailFragment.FTAG_RENAME_FILE);
                    return true;
                }
                case R.id.action_remove_file: {
                    RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(mTargetFile);
                    dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                    return true;
                }
                case R.id.action_download_file:
                case R.id.action_sync_file: {
                    mContainerActivity.getFileOperationsHelper().syncFile(mTargetFile);
                    return true;
                }
                case R.id.action_cancel_download:
                case R.id.action_cancel_upload: {
                    ((FileDisplayActivity) mContainerActivity).cancelTransference(mTargetFile);
                    return true;
                }
                case R.id.action_see_details: {
                    mContainerActivity.showDetails(mTargetFile);
                    return true;
                }
                case R.id.action_send_file: {
                    // Obtain the file
                    if (!mTargetFile.isDown()) {  // Download the file
                        Log_OC.d(TAG, mTargetFile.getRemotePath() + " : File must be downloaded");
                        ((FileDisplayActivity) mContainerActivity).startDownloadForSending(mTargetFile);

                    } else {
                        mContainerActivity.getFileOperationsHelper().sendDownloadedFile(mTargetFile);
                    }
                    return true;
                }
                case R.id.action_move: {
                    Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                    ArrayList files = new ArrayList();
                    files.add(mTargetFile);
                    action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, files);
                    getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_MOVE_FILES);
                    return true;
                }
                case R.id.action_favorite_file: {
                    mContainerActivity.getFileOperationsHelper().toggleFavorite(mTargetFile, true);
                    return true;
                }
                case R.id.action_unfavorite_file: {
                    mContainerActivity.getFileOperationsHelper().toggleFavorite(mTargetFile, false);
                    return true;
                }
                case R.id.action_copy:
                    Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                    ArrayList files = new ArrayList();
                    files.add(mTargetFile);
                    action.putExtra(FolderPickerActivity.EXTRA_FILES, files);
                    getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_COPY_FILES);
                    return true;
                default:
                    return false;
            }
        } else {
            ArrayList<OCFile> mTargetFiles = mAdapter.getCheckedItems();

            switch (menuId) {
                case R.id.action_remove_file: {
                    RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(mTargetFiles);
                    dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                    return true;
                }
                case R.id.action_download_file:
                case R.id.action_sync_file: {
                    mContainerActivity.getFileOperationsHelper().syncFiles(mTargetFiles);
                    return true;
                }
                case R.id.action_move: {
                    Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                    action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, mTargetFiles);
                    getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_MOVE_FILES);
                    return true;
                }
                case R.id.action_favorite_file: {
                    mContainerActivity.getFileOperationsHelper().toggleFavorites(mTargetFiles, true);
                    return true;
                }
                case R.id.action_unfavorite_file: {
                    mContainerActivity.getFileOperationsHelper().toggleFavorites(mTargetFiles, false);
                    return true;
                }
                case R.id.action_copy:
                    Intent action = new Intent(getActivity(), FolderPickerActivity.class);
                    action.putParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES, mTargetFiles);
                    getActivity().startActivityForResult(action, FileDisplayActivity.ACTION_COPY_FILES);
                    return true;
                default:
                    return false;
            }
        }

    }
    
    /**
     * {@inhericDoc}
     */
    @Override
    public boolean onContextItemSelected (MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        boolean matched = onFileActionChosen(item.getItemId());
        if(!matched) {
            return super.onContextItemSelected(item);
        } else {
            return matched;
        }
    }


    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     *
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile() {
        return mFile;
    }

    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile)} with a null parameter
     */
    public void listDirectory(/*boolean onlyOnDevice*/){
        listDirectory(null);
        // TODO Enable when "On Device" is recovered ?
        // listDirectory(null, onlyOnDevice);
    }
    
    public void refreshDirectory(){
        // TODO Enable when "On Device" is recovered ?
        listDirectory(getCurrentFile()/*, MainApp.getOnlyOnDevice()*/);
    }

    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     *
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory/*, boolean onlyOnDevice*/) {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {

            // Check input parameters for null
            if (directory == null) {
                if (mFile != null) {
                    directory = mFile;
                } else {
                    directory = storageManager.getFileByPath("/");
                    if (directory == null) return; // no files, wait for sync
                }
            }


            // If that's not a directory -> List its parent
            if (!directory.isFolder()) {
                Log_OC.w(TAG, "You see, that is not a directory -> " + directory.toString());
                directory = storageManager.getFileById(directory.getParentId());
            }

            // TODO Enable when "On Device" is recovered ?
            mAdapter.swapDirectory(directory, storageManager/*, onlyOnDevice*/);
            if (mFile == null || !mFile.equals(directory)) {
                mCurrentListView.setSelection(0);
            }
            mFile = directory;

            updateLayout();

        }
    }

    private void updateLayout() {
        if (!mJustFolders) {
            int filesCount = 0, foldersCount = 0, imagesCount = 0;
            int count = mAdapter.getCount();
            OCFile file;
            for (int i=0; i < count ; i++) {
                file = (OCFile) mAdapter.getItem(i);
                if (file.isFolder()) {
                    foldersCount++;
                } else {
                    if (!file.isHidden()) {
                        filesCount++;

                        if (file.isImage()) {
                            imagesCount++;
                        }
                    }
                }
            }
            // set footer text
            setFooterText(generateFooterText(filesCount, foldersCount));

            // decide grid vs list view
            OwnCloudVersion version = AccountUtils.getServerVersion(
                    ((FileActivity)mContainerActivity).getAccount());
            if (version != null && version.supportsRemoteThumbnails() &&
                imagesCount > 0 && imagesCount == filesCount) {
                switchToGridView();
                registerLongClickListener();
            } else {
                switchToListView();
            }
        }
    }

    private String generateFooterText(int filesCount, int foldersCount) {
        String output;
        if (filesCount <= 0) {
            if (foldersCount <= 0) {
                output = "";

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__folder);

            } else { // foldersCount > 1
                output = getResources().getString(R.string.file_list__footer__folders, foldersCount);
            }

        } else if (filesCount == 1) {
            if (foldersCount <= 0) {
                output = getResources().getString(R.string.file_list__footer__file);

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__file_and_folder);

            } else { // foldersCount > 1
                output = getResources().getString(R.string.file_list__footer__file_and_folders, foldersCount);
            }
        } else {    // filesCount > 1
            if (foldersCount <= 0) {
                output = getResources().getString(R.string.file_list__footer__files, filesCount);

            } else if (foldersCount == 1) {
                output = getResources().getString(R.string.file_list__footer__files_and_folder, filesCount);

            } else { // foldersCount > 1
                output = getResources().getString(
                        R.string.file_list__footer__files_and_folders, filesCount, foldersCount
                );

            }
        }
        return output;
    }

    public void sortByName(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_NAME, descending);
    }

    public void sortByDate(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_DATE, descending);
    }

    public void sortBySize(boolean descending) {
        mAdapter.setSortOrder(FileStorageUtils.SORT_SIZE, descending);
    }
}
