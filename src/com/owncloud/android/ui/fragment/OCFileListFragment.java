/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import java.util.ArrayList;
import java.util.List;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.FragmentListView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;
import com.owncloud.android.ui.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

import android.accounts.Account;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * A Fragment that lists all files and folders in a given path.
 * 
 * @author Bartek Przybylski
 * 
 */
public class OCFileListFragment extends FragmentListView implements EditNameDialogListener, ConfirmationDialogFragmentListener {
    private static final String TAG = "FileListFragment";
    private static final String SAVED_LIST_POSITION = "LIST_POSITION"; 
    
    private OCFileListFragment.ContainerActivity mContainerActivity;
    
    private OCFile mFile = null;
    private FileListListAdapter mAdapter;
    
    private Handler mHandler;
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
        inflater.inflate(R.menu.file_actions_menu, menu);
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        OCFile targetFile = (OCFile) mAdapter.getItem(info.position);
        List<Integer> toHide = new ArrayList<Integer>();    
        List<Integer> toDisable = new ArrayList<Integer>();  
        
        MenuItem item = null;
        if (targetFile.isDirectory()) {
            // contextual menu for folders
            toHide.add(R.id.action_open_file_with);
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_cancel_download);
            toHide.add(R.id.action_cancel_upload);
            toHide.add(R.id.action_see_details);
            if (    mContainerActivity.getFileDownloaderBinder().isDownloading(AccountUtils.getCurrentOwnCloudAccount(getActivity()), targetFile) ||
                    mContainerActivity.getFileUploaderBinder().isUploading(AccountUtils.getCurrentOwnCloudAccount(getActivity()), targetFile)           ) {
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                
            }
            
        } else {
            // contextual menu for regular files
            if (targetFile.isDown()) {
                toHide.add(R.id.action_cancel_download);
                toHide.add(R.id.action_cancel_upload);
                item = menu.findItem(R.id.action_download_file);
                if (item != null) {
                    item.setTitle(R.string.filedetails_sync_file);
                }
            } else {
                toHide.add(R.id.action_open_file_with);
            }
            if ( mContainerActivity.getFileDownloaderBinder().isDownloading(AccountUtils.getCurrentOwnCloudAccount(getActivity()), targetFile)) {
                toHide.add(R.id.action_download_file);
                toHide.add(R.id.action_cancel_upload);
                toDisable.add(R.id.action_open_file_with);
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                    
            } else if ( mContainerActivity.getFileUploaderBinder().isUploading(AccountUtils.getCurrentOwnCloudAccount(getActivity()), targetFile)) {
                toHide.add(R.id.action_download_file);
                toHide.add(R.id.action_cancel_download);
                toDisable.add(R.id.action_open_file_with);
                toDisable.add(R.id.action_rename_file);
                toDisable.add(R.id.action_remove_file);
                    
            } else {
                toHide.add(R.id.action_cancel_download);
                toHide.add(R.id.action_cancel_upload);
            }
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
            case R.id.action_rename_file: {
                EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.rename_dialog_title), mTargetFile.getFileName(), this);
                dialog.show(getFragmentManager(), EditNameDialog.TAG);
                return true;
            }
            case R.id.action_remove_file: {
                int messageStringId = R.string.confirmation_remove_alert;
                int posBtnStringId = R.string.confirmation_remove_remote;
                int neuBtnStringId = -1;
                if (mTargetFile.isDirectory()) {
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
            case R.id.action_open_file_with: {
                String storagePath = mTargetFile.getStoragePath();
                String encodedStoragePath = WebdavUtils.encodePath(storagePath);
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mTargetFile.getMimetype());
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(i);
                    
                } catch (Throwable t) {
                    Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mTargetFile.getMimetype());
                    boolean toastIt = true; 
                    String mimeType = "";
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                        if (mimeType == null || !mimeType.equals(mTargetFile.getMimetype())) {
                            if (mimeType != null) {
                                i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mimeType);
                            } else {
                                // desperate try
                                i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), "*/*");
                            }
                            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            startActivity(i);
                            toastIt = false;
                        }
                        
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                        
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                        
                    } catch (Throwable th) {
                        Log.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                        
                    } finally {
                        if (toastIt) {
                            Toast.makeText(getActivity(), "There is no application to handle file " + mTargetFile.getFileName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                }
                return true;
            }
            case R.id.action_download_file: {
                Account account = AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity());
                RemoteOperation operation = new SynchronizeFileOperation(mTargetFile, null, mContainerActivity.getStorageManager(), account, true, false, getSherlockActivity());
                WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(account, getSherlockActivity().getApplicationContext());
                operation.execute(wc, mContainerActivity, mHandler);
                getSherlockActivity().showDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
                return true;
            }
            case R.id.action_cancel_download: {
                FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
                Account account = AccountUtils.getCurrentOwnCloudAccount(getActivity());
                if (downloaderBinder != null && downloaderBinder.isDownloading(account, mTargetFile)) {
                    downloaderBinder.cancel(account, mTargetFile);
                    listDirectory();
                    mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
                }
                return true;
            }
            case R.id.action_cancel_upload: {
                FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
                Account account = AccountUtils.getCurrentOwnCloudAccount(getActivity());
                if (uploaderBinder != null && uploaderBinder.isUploading(account, mTargetFile)) {
                    uploaderBinder.cancel(account, mTargetFile);
                    listDirectory();
                    mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
                }
                return true;
            }
            case R.id.action_see_details: {
                ((FileFragment.ContainerActivity)getActivity()).showFragmentWithDetails(mTargetFile);
                return true;
            }
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
    public interface ContainerActivity extends TransferServiceGetter, OnRemoteOperationListener {

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
        
        
        /**
         * Callback method invoked when a the 'transfer state' of a file changes.
         * 
         * This happens when a download or upload is started or ended for a file.
         * 
         * This method is necessary by now to update the user interface of the double-pane layout in tablets
         * because methods {@link FileDownloaderBinder#isDownloading(Account, OCFile)} and {@link FileUploaderBinder#isUploading(Account, OCFile)}
         * won't provide the needed response before the method where this is called finishes. 
         * 
         * TODO Remove this when the transfer state of a file is kept in the database (other thing TODO)
         * 
         * @param file          OCFile which state changed.
         * @param downloading   Flag signaling if the file is now downloading.
         * @param uploading     Flag signaling if the file is now uploading.
         */
        public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading);
        
    }
    
    
    @Override
    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            RemoteOperation operation = new RenameFileOperation(mTargetFile, 
                                                                AccountUtils.getCurrentOwnCloudAccount(getActivity()), 
                                                                newFilename, 
                                                                mContainerActivity.getStorageManager());
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(AccountUtils.getCurrentOwnCloudAccount(getSherlockActivity()), getSherlockActivity().getApplicationContext());
            operation.execute(wc, mContainerActivity, mHandler);
            getActivity().showDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
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
                operation.execute(wc, mContainerActivity, mHandler);
                
                getActivity().showDialog(FileDisplayActivity.DIALOG_SHORT_WAIT);
            }
        }
    }
    
    @Override
    public void onNeutral(String callerTag) {
        File f = null;
        if (mTargetFile.isDirectory()) {
            // TODO run in a secondary thread?
            mContainerActivity.getStorageManager().removeDirectory(mTargetFile, false, true);
            
        } else if (mTargetFile.isDown() && (f = new File(mTargetFile.getStoragePath())).exists()) {
            f.delete();
            mTargetFile.setStoragePath(null);
            mContainerActivity.getStorageManager().saveFile(mTargetFile);
        }
        listDirectory();
        mContainerActivity.onTransferStateChanged(mTargetFile, false, false);
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log.d(TAG, "REMOVAL CANCELED");
    }


}
