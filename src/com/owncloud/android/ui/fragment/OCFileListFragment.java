/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.utils.Log_OC;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * A Fragment that lists all files and folders in a given path.
 * 
 * TODO refactorize to get rid of direct dependency on FileDisplayActivity
 * 
 * @author Bartek Przybylski
 * @author masensio
 * @author David A. Velasco
 */
public class OCFileListFragment extends ExtendedListFragment implements EditNameDialogListener, ConfirmationDialogFragmentListener {
    
    private static final String TAG = OCFileListFragment.class.getSimpleName();

    private static final String MY_PACKAGE = OCFileListFragment.class.getPackage() != null ? OCFileListFragment.class.getPackage().getName() : "com.owncloud.android.ui.fragment";
    private static final String EXTRA_FILE = MY_PACKAGE + ".extra.FILE";

    private static final String KEY_INDEXES = "INDEXES";
    private static final String KEY_FIRST_POSITIONS= "FIRST_POSITIONS";
    private static final String KEY_TOPS = "TOPS";
    private static final String KEY_HEIGHT_CELL = "HEIGHT_CELL";
    
    private FileFragment.ContainerActivity mContainerActivity;
   
    private OCFile mFile = null;
    private FileListListAdapter mAdapter;
    
    private OCFile mTargetFile;

    // Save the state of the scroll in browsing
    private ArrayList<Integer> mIndexes;
    private ArrayList<Integer> mFirstPositions;
    private ArrayList<Integer> mTops;

    private int mHeightCell = 0;
    
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
    }

    
    @Override
    public void onDetach() {
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

        mAdapter = new FileListListAdapter(getSherlockActivity(), mContainerActivity); 
                
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(EXTRA_FILE);
            mIndexes = savedInstanceState.getIntegerArrayList(KEY_INDEXES);
            mFirstPositions = savedInstanceState.getIntegerArrayList(KEY_FIRST_POSITIONS);
            mTops = savedInstanceState.getIntegerArrayList(KEY_TOPS);
            mHeightCell = savedInstanceState.getInt(KEY_HEIGHT_CELL);
            
        } else {
            mIndexes = new ArrayList<Integer>();
            mFirstPositions = new ArrayList<Integer>();
            mTops = new ArrayList<Integer>();
            mHeightCell = 0;
            
        }
        
        mAdapter = new FileListListAdapter(getActivity(), mContainerActivity);
        
        setListAdapter(mAdapter);
        
        registerForContextMenu(getListView());
        getListView().setOnCreateContextMenuListener(this);        
        
 //       mHandler = new Handler();
  }
    
    /**
     * Saves the current listed folder.
     */
    @Override
    public void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_FILE, mFile);
        outState.putIntegerArrayList(KEY_INDEXES, mIndexes);
        outState.putIntegerArrayList(KEY_FIRST_POSITIONS, mFirstPositions);
        outState.putIntegerArrayList(KEY_TOPS, mTops);
        outState.putInt(KEY_HEIGHT_CELL, mHeightCell);
    }
    
    /**
     * Call this, when the user presses the up button.
     * 
     * Tries to move up the current folder one level. If the parent folder was removed from the database, 
     * it continues browsing up until finding an existing folders.
     * 
     * return       Count of folder levels browsed up.
     */
    public int onBrowseUp() {
        OCFile parentDir = null;
        int moveCount = 0;
        
        if(mFile != null){
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            
            String parentPath = null;
            if (mFile.getParentId() != FileDataStorageManager.ROOT_PARENT_ID) {
                parentPath = new File(mFile.getRemotePath()).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            } else {
                parentDir = storageManager.getFileByPath(OCFile.ROOT_PATH);    // never returns null; keep the path in root folder
            }
            while (parentDir == null) {
                parentPath = new File(parentPath).getParent();
                parentPath = parentPath.endsWith(OCFile.PATH_SEPARATOR) ? parentPath : parentPath + OCFile.PATH_SEPARATOR;
                parentDir = storageManager.getFileByPath(parentPath);
                moveCount++;
            }   // exit is granted because storageManager.getFileByPath("/") never returns null
            mFile = parentDir;           
        }
        
        if (mFile != null) {
            listDirectory(mFile);

            ((FileDisplayActivity)mContainerActivity).startSyncFolderOperation(mFile);
            
            // restore index and top position
            restoreIndexAndTopPosition();
            
        }   // else - should never happen now
   
        return moveCount;
    }
    
    /*
     * Restore index and position
     */
    private void restoreIndexAndTopPosition() {
        if (mIndexes.size() > 0) {  
            // needs to be checked; not every browse-up had a browse-down before 
            
            int index = mIndexes.remove(mIndexes.size() - 1);
            
            int firstPosition = mFirstPositions.remove(mFirstPositions.size() -1);
            
            int top = mTops.remove(mTops.size() - 1);
            
            mList.setSelectionFromTop(firstPosition, top);
            
            // Move the scroll if the selection is not visible
            int indexPosition = mHeightCell*index;
            int height = mList.getHeight();
            
            if (indexPosition > height) {
                if (android.os.Build.VERSION.SDK_INT >= 11)
                {
                    mList.smoothScrollToPosition(index); 
                }
                else if (android.os.Build.VERSION.SDK_INT >= 8)
                {
                    mList.setSelectionFromTop(index, 0);
                }
                
            }
        }
    }
    
    /*
     * Save index and top position
     */
    private void saveIndexAndTopPosition(int index) {
        
        mIndexes.add(index);
        
        int firstPosition = mList.getFirstVisiblePosition();
        mFirstPositions.add(firstPosition);
        
        View view = mList.getChildAt(0);
        int top = (view == null) ? 0 : view.getTop() ;

        mTops.add(top);
        
        // Save the height of a cell
        mHeightCell = (view == null || mHeightCell != 0) ? mHeightCell : view.getHeight();
    }
    
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        OCFile file = (OCFile) mAdapter.getItem(position);
        if (file != null) {
            if (file.isFolder()) { 
                // update state and view of this fragment
                listDirectory(file);
                // then, notify parent activity to let it update its state and view, and other fragments
                mContainerActivity.onBrowsedDownTo(file);
                // save index and top position
                saveIndexAndTopPosition(position);
                
            } else { /// Click on a file
                if (PreviewImageFragment.canBePreviewed(file)) {
                    // preview image - it handles the download, if needed
                    ((FileDisplayActivity)mContainerActivity).startImagePreview(file);
                    
                } else if (file.isDown()) {
                    if (PreviewMediaFragment.canBePreviewed(file)) {
                        // media preview
                        ((FileDisplayActivity)mContainerActivity).startMediaPreview(file, 0, true);
                    } else {
                        ((FileDisplayActivity)mContainerActivity).getFileOperationsHelper().openFile(file);
                    }
                    
                } else {
                    // automatic download, preview on finish
                    ((FileDisplayActivity)mContainerActivity).startDownloadForPreview(file);
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
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getSherlockActivity().getMenuInflater();
        inflater.inflate(R.menu.file_actions_menu, menu);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        OCFile targetFile = (OCFile) mAdapter.getItem(info.position);
        List<Integer> toHide = new ArrayList<Integer>();    
        List<Integer> toDisable = new ArrayList<Integer>();  
        
        MenuItem item = null;
        if (targetFile.isFolder()) {
            // contextual menu for folders
            toHide.add(R.id.action_open_file_with);
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_cancel_download);
            toHide.add(R.id.action_cancel_upload);
            toHide.add(R.id.action_sync_file);
            toHide.add(R.id.action_see_details);
            toHide.add(R.id.action_send_file);
            if (    mContainerActivity.getFileDownloaderBinder().isDownloading(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), targetFile) ||
                    mContainerActivity.getFileUploaderBinder().isUploading(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), targetFile)           ) {
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                
            }
            
        } else {
            // contextual menu for regular files
            
            // new design: 'download' and 'open with' won't be available anymore in context menu
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_open_file_with);
            
            if (targetFile.isDown()) {
                toHide.add(R.id.action_cancel_download);
                toHide.add(R.id.action_cancel_upload);
                
            } else {
                toHide.add(R.id.action_sync_file);
            }
            if ( mContainerActivity.getFileDownloaderBinder().isDownloading(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), targetFile)) {
                toHide.add(R.id.action_cancel_upload);
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                    
            } else if ( mContainerActivity.getFileUploaderBinder().isUploading(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), targetFile)) {
                toHide.add(R.id.action_cancel_download);
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                    
            } else {
                toHide.add(R.id.action_cancel_download);
                toHide.add(R.id.action_cancel_upload);
            }
        }
        
        // Options shareLink
        if (!targetFile.isShareByLink()) {
            toHide.add(R.id.action_unshare_file);
        }

        // Send file
        boolean sendEnabled = getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on");
        if (!sendEnabled) {
            toHide.add(R.id.action_send_file);
        }
        
        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
        
        for (int i : toDisable) {
            item = menu.findItem(i);
            if (item != null) {
                item.setEnabled(false);
            }
        }
    }
    
    
    /**
     * {@inhericDoc}
     */
    @Override
    public boolean onContextItemSelected (MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();        
        mTargetFile = (OCFile) mAdapter.getItem(info.position);
        switch (item.getItemId()) {                
            case R.id.action_share_file: {
                mContainerActivity.getFileOperationsHelper().shareFileWithLink(mTargetFile);
                return true;
            }
            case R.id.action_unshare_file: {
                mContainerActivity.getFileOperationsHelper().unshareFileWithLink(mTargetFile);
                return true;
            }
            case R.id.action_rename_file: {
                String fileName = mTargetFile.getFileName();
                int extensionStart = mTargetFile.isFolder() ? -1 : fileName.lastIndexOf(".");
                int selectionEnd = (extensionStart >= 0) ? extensionStart : fileName.length();
                EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.rename_dialog_title), fileName, 0, selectionEnd, this);
                dialog.show(getFragmentManager(), EditNameDialog.TAG);
                return true;
            }
            case R.id.action_remove_file: {
                int messageStringId = R.string.confirmation_remove_alert;
                int posBtnStringId = R.string.confirmation_remove_remote;
                int neuBtnStringId = -1;
                if (mTargetFile.isFolder()) {
                    messageStringId = R.string.confirmation_remove_folder_alert;
                    posBtnStringId = R.string.confirmation_remove_remote_and_local;
                    neuBtnStringId = R.string.confirmation_remove_folder_local;
                } else if (mTargetFile.isDown()) {
                    posBtnStringId = R.string.confirmation_remove_remote_and_local;
                    neuBtnStringId = R.string.confirmation_remove_local;
                }
                ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                        messageStringId,
                        new String[]{mTargetFile.getFileName()},
                        posBtnStringId,
                        neuBtnStringId,
                        R.string.common_cancel);
                confDialog.setOnConfirmationListener(this);
                confDialog.show(getFragmentManager(), FileDetailFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(mTargetFile);
                return true;
            }
            case R.id.action_cancel_download: {
                FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
                Account account = AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity());
                if (downloaderBinder != null && downloaderBinder.isDownloading(account, mTargetFile)) {
                    downloaderBinder.cancel(account, mTargetFile);
                    listDirectory();
                    mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
                }
                return true;
            }
            case R.id.action_cancel_upload: {
                FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
                Account account = AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity());
                if (uploaderBinder != null && uploaderBinder.isUploading(account, mTargetFile)) {
                    uploaderBinder.cancel(account, mTargetFile);
                    listDirectory();
                    mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
                }
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
                    ((FileDisplayActivity)mContainerActivity).startDownloadForSending(mTargetFile);
                    
                } else {
                    ((FileDisplayActivity)mContainerActivity).getFileOperationsHelper().sendDownloadedFile(mTargetFile);
                }
                return true;
            }
            default:
                return super.onContextItemSelected(item); 
        }
    }


    /**
     * Use this to query the {@link OCFile} that is currently
     * being displayed by this fragment
     * @return The currently viewed OCFile
     */
    public OCFile getCurrentFile(){
        return mFile;
    }
    
    /**
     * Calls {@link OCFileListFragment#listDirectory(OCFile)} with a null parameter
     */
    public void listDirectory(){
        listDirectory(null);
    }
    
    /**
     * Lists the given directory on the view. When the input parameter is null,
     * it will either refresh the last known directory. list the root
     * if there never was a directory.
     * 
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory) {
        FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
        if (storageManager != null) {

            // Check input parameters for null
            if(directory == null){
                if(mFile != null){
                    directory = mFile;
                } else {
                    directory = storageManager.getFileByPath("/");
                    if (directory == null) return; // no files, wait for sync
                }
            }
        
        
            // If that's not a directory -> List its parent
            if(!directory.isFolder()){
                Log_OC.w(TAG, "You see, that is not a directory -> " + directory.toString());
                directory = storageManager.getFileById(directory.getParentId());
            }

            mAdapter.swapDirectory(directory, storageManager);
            if (mFile == null || !mFile.equals(directory)) {
                mList.setSelectionFromTop(0, 0);
            }
            mFile = directory;
        }
    }
    
    
    
    @Override
    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log_OC.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            mContainerActivity.getFileOperationsHelper().renameFile(mTargetFile, newFilename);
        }
    }

    
    @Override
    public void onConfirmation(String callerTag) {
        if (callerTag.equals(FileDetailFragment.FTAG_CONFIRMATION)) {
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            if (storageManager.getFileById(mTargetFile.getFileId()) != null) {
                mContainerActivity.getFileOperationsHelper().removeFile(mTargetFile, true);
            }
        }
    }
    
    @Override
    public void onNeutral(String callerTag) {
        mContainerActivity.getStorageManager().removeFile(mTargetFile, false, true);    // TODO perform in background task / new thread
        listDirectory();
        mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log_OC.d(TAG, "REMOVAL CANCELED");
    }

}
