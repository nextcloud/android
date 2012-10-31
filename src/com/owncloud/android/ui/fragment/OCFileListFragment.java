/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.ui.FragmentListView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import eu.alefzero.webdav.WebdavClient;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * A Fragment that lists all files and folders in a given path.
 * 
 * @author Bartek Przybylski
 * 
 */
public class OCFileListFragment extends FragmentListView implements EditNameDialog.EditNameDialogListener, OnRemoteOperationListener, ConfirmationDialogFragmentListener {
    private static final String TAG = "FileListFragment";
    private static final String SAVED_LIST_POSITION = "LIST_POSITION"; 
    
    private OCFileListFragment.ContainerActivity mContainerActivity;
    
    private OCFile mFile = null;
    private FileListListAdapter mAdapter;
    
    private Handler mHandler;
    private boolean mIsLargeLayout;
    private OCFile mTargetFile;

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + OCFileListFragment.ContainerActivity.class.getSimpleName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated() start");
        
        super.onActivityCreated(savedInstanceState);
        mAdapter = new FileListListAdapter(mContainerActivity.getInitialDirectory(), mContainerActivity.getStorageManager(), getActivity(), mContainerActivity);
        setListAdapter(mAdapter);
        
        if (savedInstanceState != null) {
            Log.i(TAG, "savedInstanceState is not null");
            int position = savedInstanceState.getInt(SAVED_LIST_POSITION);
            setReferencePosition(position);
        }
        
        registerForContextMenu(getListView());
        getListView().setOnCreateContextMenuListener(this);        
        
        mIsLargeLayout = getResources().getBoolean(R.bool.large_layout);
        mHandler = new Handler();
        
        Log.i(TAG, "onActivityCreated() stop");
    }
    
    

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState() start");
        
        savedInstanceState.putInt(SAVED_LIST_POSITION, getReferencePosition());
        
        Log.i(TAG, "onSaveInstanceState() stop");
    }
    
    
    @Override
    public void onItemClick(AdapterView<?> l, View v, int position, long id) {
        OCFile file = (OCFile) mAdapter.getItem(position);
        if (file != null) {
            /// Click on a directory
            if (file.getMimetype().equals("DIR")) {
                // just local updates
                mFile = file;
                listDirectory(file);
                // any other updates are let to the container Activity
                mContainerActivity.onDirectoryClick(file);
            
            } else {    /// Click on a file
                mContainerActivity.onFileClick(file);
            }
            
        } else {
            Log.d(TAG, "Null object in ListAdapter!!");
        }
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.file_context_menu, menu);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;        
    }
    
    
    /**
     * {@inhericDoc}
     */
    @Override
    public boolean onContextItemSelected (MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();        
        mTargetFile = (OCFile) mAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.rename_file_item:
                EditNameDialog dialog = EditNameDialog.newInstance(mTargetFile.getFileName());
                dialog.setOnDismissListener(this);
                dialog.show(getFragmentManager(), "nameeditdialog");
                Log.d(TAG, "RENAME SELECTED, item " + info.id + " at position " + info.position);
                return true;
            case R.id.remove_file_item:
                ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                        R.string.confirmation_remove_alert,
                        new String[]{mTargetFile.getFileName()},
                        mTargetFile.isDown() ? R.string.confirmation_remove_remote_and_local : R.string.confirmation_remove_remote,
                        mTargetFile.isDown() ? R.string.confirmation_remove_local : -1,
                        R.string.common_cancel);
                confDialog.setOnConfirmationListener(this);
                confDialog.show(getFragmentManager(), FileDetailFragment.FTAG_CONFIRMATION);
                Log.d(TAG, "REMOVE SELECTED, item " + info.id + " at position " + info.position);
                return true;
            default:
                return super.onContextItemSelected(item); 
        }
    }
    

    /**
     * Call this, when the user presses the up button
     */
    public void onNavigateUp() {
        OCFile parentDir = null;
        
        if(mFile != null){
            DataStorageManager storageManager = mContainerActivity.getStorageManager();
            parentDir = storageManager.getFileById(mFile.getParentId());
            mFile = parentDir;
        }
        listDirectory(parentDir);
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
     * it will either refresh the last known directory, or list the root
     * if there never was a directory.
     * 
     * @param directory File to be listed
     */
    public void listDirectory(OCFile directory) {
        DataStorageManager storageManager = mContainerActivity.getStorageManager();
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
            if(!directory.isDirectory()){
                Log.w(TAG, "You see, that is not a directory -> " + directory.toString());
                directory = storageManager.getFileById(directory.getParentId());
            }

            mAdapter.swapDirectory(directory, storageManager);
            if (mFile == null || !mFile.equals(directory)) {
                mList.setSelectionFromTop(0, 0);
            }
            mFile = directory;
        }
    }
    
    
    
    /**
     * Interface to implement by any Activity that includes some instance of FileListFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity extends TransferServiceGetter {

        /**
         * Callback method invoked when a directory is clicked by the user on the files list
         *  
         * @param file
         */
        public void onDirectoryClick(OCFile file);
        
        /**
         * Callback method invoked when a file (non directory) is clicked by the user on the files list
         *  
         * @param file
         */
        public void onFileClick(OCFile file);

        /**
         * Getter for the current DataStorageManager in the container activity
         */
        public DataStorageManager getStorageManager();
        
        
        /**
         * Callback method invoked when the parent activity is fully created to get the directory to list firstly.
         * 
         * @return  Directory to list firstly. Can be NULL.
         */
        public OCFile getInitialDirectory();
        
        
    }



    @Override
    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            RemoteOperation operation = new RenameFileOperation(mTargetFile, 
                                                                newFilename, 
                                                                mContainerActivity.getStorageManager());
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), getSherlockActivity().getApplicationContext());
            operation.execute(wc, this, mHandler);
            getActivity().showDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
        }
    }


    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
                
        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation)operation, result);
        }
    }

    
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        getActivity().dismissDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            if (mIsLargeLayout) {
                // TODO - this should be done only when the current FileDetailFragment shows the deleted file
                //          -> THIS METHOD WOULD BE BETTER PLACED AT THE ACTIVITY LEVEL
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null)); // empty FileDetailFragment
                transaction.commit();
            }
            listDirectory();
                
        } else {
            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                // TODO show the SSL warning dialog
            }
        }
    }

    
    private void onRenameFileOperationFinish(RenameFileOperation operation, RemoteOperationResult result) {
        getActivity().dismissDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
        if (result.isSuccess()) {
            listDirectory();
            // TODO is file
            
        } else {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_local_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                // TODO throw again the new rename dialog
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_server_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (result.isSslRecoverableException()) {
                    // TODO show the SSL warning dialog
                }
            }
        }
    }


    @Override
    public void onConfirmation(String callerTag) {
        if (callerTag.equals(FileDetailFragment.FTAG_CONFIRMATION)) {
            if (mContainerActivity.getStorageManager().getFileById(mTargetFile.getFileId()) != null) {
                RemoteOperation operation = new RemoveFileOperation( mTargetFile, 
                                                                    true, 
                                                                    mContainerActivity.getStorageManager());
                WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), getSherlockActivity().getApplicationContext());
                operation.execute(wc, this, mHandler);
                
                getActivity().showDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
            }
        }
    }
    
    @Override
    public void onNeutral(String callerTag) {
        File f = null;
        if (mTargetFile.isDown() && (f = new File(mTargetFile.getStoragePath())).exists()) {
            f.delete();
            mTargetFile.setStoragePath(null);
            mContainerActivity.getStorageManager().saveFile(mFile);
        }
        listDirectory();
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log.d(TAG, "REMOVAL CANCELED");
    }
    
    
}
