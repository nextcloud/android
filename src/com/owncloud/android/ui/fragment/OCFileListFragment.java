/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

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
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.FileActionsDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;

/**
 * A Fragment that lists all files and folders in a given path.
 *
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
public class OCFileListFragment extends ExtendedListFragment
        implements FileActionsDialogFragment.FileActionsDialogFragmentListener {

    private static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ?
            OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";

    public final static String ARG_JUST_FOLDERS = MY_PACKAGE + ".JUST_FOLDERS";
    public final static String ARG_ALLOW_CONTEXTUAL_ACTIONS = MY_PACKAGE + ".ALLOW_CONTEXTUAL";
    public final static String ARG_HIDE_FAB = MY_PACKAGE + ".HIDE_FAB";

    private static final String KEY_FILE = MY_PACKAGE + ".extra.FILE";
    private static final String KEY_FAB_EVER_CLICKED = "FAB_EVER_CLICKED";

    private static final String GRID_IS_PREFERED_PREFERENCE = "gridIsPrefered";

    private static String DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER";

    private FileFragment.ContainerActivity mContainerActivity;

    private OCFile mFile = null;
    private FileListListAdapter mAdapter;
    private boolean mJustFolders;

    private OCFile mTargetFile;

    private boolean miniFabClicked = false;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.i(TAG, "onCreateView() start");
        View v = super.onCreateView(inflater, container, savedInstanceState);

        Log_OC.i(TAG, "onCreateView() end");
        return v;
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

        boolean hideFab = (args != null) && args.getBoolean(ARG_HIDE_FAB, false);
        if (hideFab) {
            setFabEnabled(false);
        } else {
            setFabEnabled(true);
            registerFabListeners();

            // detect if a mini FAB has ever been clicked
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            if(prefs.getLong(KEY_FAB_EVER_CLICKED, 0) > 0) {
                miniFabClicked = true;
            }

            // add labels to the min FABs when none of them has ever been clicked on
            if(!miniFabClicked) {
                setFabLabels();
            } else {
                removeFabLabels();
            }
        }
  }

    /**
     * adds labels to all mini FABs.
     */
    private void setFabLabels() {
        getFabUpload().setTitle(getResources().getString(R.string.actionbar_upload));
        getFabMkdir().setTitle(getResources().getString(R.string.actionbar_mkdir));
        getFabUploadFromApp().setTitle(getResources().getString(R.string.actionbar_upload_from_apps));
    }

    /**
     * registers all listeners on all mini FABs.
     */
    private void registerFabListeners() {
        registerFabUploadListeners();
        registerFabMkDirListeners();
        registerFabUploadFromAppListeners();
    }

    /**
     * registers {@link android.view.View.OnClickListener} and {@link android.view.View.OnLongClickListener}
     * on the Upload mini FAB for the linked action and {@link Toast} showing the underlying action.
     */
    private void registerFabUploadListeners() {
        getFabUpload().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadFilesActivity.startUploadActivityForResult(getActivity(), ((FileActivity)getActivity())
                        .getAccount(), FileDisplayActivity.REQUEST_CODE__SELECT_MULTIPLE_FILES);
                getFabMain().collapse();
                recordMiniFabClick();
            }
        });

        getFabUpload().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), R.string.actionbar_upload, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    /**
     * registers {@link android.view.View.OnClickListener} and {@link android.view.View.OnLongClickListener}
     * on the 'Create Dir' mini FAB for the linked action and {@link Toast} showing the underlying action.
     */
    private void registerFabMkDirListeners() {
        getFabMkdir().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateFolderDialogFragment dialog =
                        CreateFolderDialogFragment.newInstance(mFile);
                dialog.show(getActivity().getSupportFragmentManager(), DIALOG_CREATE_FOLDER);
                getFabMain().collapse();
                recordMiniFabClick();
            }
        });

        getFabMkdir().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(), R.string.actionbar_mkdir, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    /**
     * registers {@link android.view.View.OnClickListener} and {@link android.view.View.OnLongClickListener}
     * on the Upload from App mini FAB for the linked action and {@link Toast} showing the underlying action.
     */
    private void registerFabUploadFromAppListeners() {
        getFabUploadFromApp().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                //Intent.EXTRA_ALLOW_MULTIPLE is only supported on api level 18+, Jelly Bean
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                getActivity().startActivityForResult(
                        Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                        FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
                );
                getFabMain().collapse();
                recordMiniFabClick();
            }
        });

        getFabUploadFromApp().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getActivity(),
                        R.string.actionbar_upload_from_apps,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    /**
     * records a click on a mini FAB and thus:
     * <ol>
     *     <li>persists the click fact</li>
     *     <li>removes the mini FAB labels</li>
     * </ol>
     */
    private void recordMiniFabClick() {
        // only record if it hasn't been done already at some other time
        if(!miniFabClicked) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            sp.edit().putLong(KEY_FAB_EVER_CLICKED, 1).commit();
            miniFabClicked = true;
        }
    }

    /**
     * removes the labels on all known min FABs.
     */
    private void removeFabLabels() {
        getFabUpload().setTitle(null);
        getFabMkdir().setTitle(null);
        getFabUploadFromApp().setTitle(null);
        ((TextView) getFabUpload().getTag(
                com.getbase.floatingactionbutton.R.id.fab_label)).setVisibility(View.GONE);
        ((TextView) getFabMkdir().getTag(
                com.getbase.floatingactionbutton.R.id.fab_label)).setVisibility(View.GONE);
        ((TextView) getFabUploadFromApp().getTag(
                com.getbase.floatingactionbutton.R.id.fab_label)).setVisibility(View.GONE);
    }

    private void registerLongClickListener() {
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int index, long arg3) {
                showFileAction(index);
                return true;
            }
        });
    }


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

            FileActionsDialogFragment dialog = FileActionsDialogFragment.newInstance(menu,
                    fileIndex, targetFile.getFileName());
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

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        changeGridIcon(menu);   // this is enough if the option stays out of the action bar
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
    @Override
    public void onCreateContextMenu(
            ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Bundle args = getArguments();
        boolean allowContextualActions =
                (args == null) ? true : args.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        if (allowContextualActions) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.file_actions_menu, menu);
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
            OCFile targetFile = (OCFile) mAdapter.getItem(info.position);

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

//            String.format(mContext.getString(R.string.subject_token),
//                    getClient().getCredentials().getUsername(), file.getFileName()));

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onFileActionChosen(int menuId, int filePosition) {
        mTargetFile = (OCFile) mAdapter.getItem(filePosition);
        switch (menuId) {
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().showShareFile(mTargetFile);
                return true;
            }
            case R.id.action_open_file_with: {
                mContainerActivity.getFileOperationsHelper().openFile(mTargetFile);
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
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity)mContainerActivity).cancelTransference(mTargetFile);
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

                // Pass mTargetFile that contains info of selected file/folder
                action.putExtra(FolderPickerActivity.EXTRA_FILE, mTargetFile);
                getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_FILES);
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

                // Pass mTargetFile that contains info of selected file/folder
                action.putExtra(FolderPickerActivity.EXTRA_FILE, mTargetFile);
                getActivity().startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__COPY_FILES);
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inhericDoc}
     */
    @Override
    public boolean onContextItemSelected (MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        boolean matched = onFileActionChosen(item.getItemId(),
                ((AdapterContextMenuInfo) item.getMenuInfo()).position);
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
                    isGridViewPreferred(mFile)) {
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

    /**
     * Determines if user set folder to grid or list view. If folder is not set itself,
     * it finds a parent that is set (at least root is set).
     * @param file
     * @return
     */
    public boolean isGridViewPreferred(OCFile file){
        if (file != null) {
            OCFile fileToTest = file;
            OCFile parentDir = null;
            String parentPath = null;
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();

            SharedPreferences setting =
                    getActivity().getSharedPreferences(
                            GRID_IS_PREFERED_PREFERENCE, Context.MODE_PRIVATE
                    );

            if (setting.contains(String.valueOf(fileToTest.getFileId()))) {
                return setting.getBoolean(String.valueOf(fileToTest.getFileId()), false);
            } else {
                do {
                    if (fileToTest.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                        parentPath = new File(fileToTest.getRemotePath()).getParent();
                        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                                parentPath + OCFile.PATH_SEPARATOR;
                        parentDir = storageManager.getFileByPath(parentPath);
                    } else {
                        parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);
                    }

                    while (parentDir == null) {
                        parentPath = new File(parentPath).getParent();
                        parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath :
                                parentPath + OCFile.PATH_SEPARATOR;
                        parentDir = storageManager.getFileByPath(parentPath);
                    }
                    fileToTest = parentDir;
                } while (endWhile(parentDir, setting));
                return setting.getBoolean(String.valueOf(fileToTest.getFileId()), false);
            }
        } else {
            return false;
        }
    }

    private boolean endWhile(OCFile parentDir, SharedPreferences setting) {
        if (parentDir.getRemotePath().compareToIgnoreCase(OCFile.ROOT_PATH) == 0) {
            return false;
        } else {
            return !setting.contains(String.valueOf(parentDir.getFileId()));
        }
    }

    private void changeGridIcon(Menu menu){
        MenuItem menuItem = menu.findItem(R.id.action_switch_view);
        if (isGridViewPreferred(mFile)){
            menuItem.setTitle(getString(R.string.action_switch_list_view));
            menuItem.setIcon(R.drawable.ic_view_list);
        } else {
            menuItem.setTitle(getString(R.string.action_switch_grid_view));
            menuItem.setIcon(R.drawable.ic_view_module);
        }
    }

    public void setListAsPreferred() {
        saveGridAsPreferred(false);
        switchToListView();
    }

    public void setGridAsPreferred() {
        saveGridAsPreferred(true);
        switchToGridView();
    }

    private void saveGridAsPreferred(boolean setGrid){
        SharedPreferences setting = getActivity().getSharedPreferences(
                GRID_IS_PREFERED_PREFERENCE, Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = setting.edit();
        editor.putBoolean(String.valueOf(mFile.getFileId()), setGrid);
        editor.apply();
    }


}
