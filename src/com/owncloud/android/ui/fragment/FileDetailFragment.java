/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileObserverService;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.operations.common.OnRemoteOperationListener;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;


/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileDetailFragment extends FileFragment implements
        OnClickListener, 
        ConfirmationDialogFragment.ConfirmationDialogFragmentListener, OnRemoteOperationListener, EditNameDialogListener {

    private FileFragment.ContainerActivity mContainerActivity;
    
    private int mLayout;
    private View mView;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    
    private UploadFinishReceiver mUploadFinishReceiver;
    public ProgressListener mProgressListener;
    
    private Handler mHandler;
    private RemoteOperation mLastRemoteOperation;
    
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    

    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        super();
        mAccount = null;
        mStorageManager = null;
        mLayout = R.layout.file_details_empty;
        mProgressListener = null;
    }
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FileDetailFragment(OCFile fileToDetail, Account ocAccount) {
        super(fileToDetail);
        mAccount = ocAccount;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
        mLayout = R.layout.file_details_empty;
        mProgressListener = null;
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setHasOptionsMenu(true);
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            setFile((OCFile)savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }
        
        if(getFile() != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
        
        View view = null;
        //view = inflater.inflate(mLayout, container, false);
        view = inflater.inflate(mLayout, null);
        mView = view;
        
        if (mLayout == R.layout.file_details_fragment) {
            mView.findViewById(R.id.fdKeepInSync).setOnClickListener(this);
            ProgressBar progressBar = (ProgressBar)mView.findViewById(R.id.fdProgressBar);
            mProgressListener = new ProgressListener(progressBar);
            mView.findViewById(R.id.fdCancelBtn).setOnClickListener(this);
        }
        
        updateFileDetails(false, false);
        return view;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mContainerActivity = (ContainerActivity) activity;
            
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement " + FileDetailFragment.ContainerActivity.class.getSimpleName());
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAccount != null) {
            mStorageManager = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
        }
    }
        

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, getFile());
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mUploadFinishReceiver = new UploadFinishReceiver();
        IntentFilter filter = new IntentFilter(FileUploader.getUploadFinishMessage());
        getActivity().registerReceiver(mUploadFinishReceiver, filter);

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mUploadFinishReceiver != null) {
            getActivity().unregisterReceiver(mUploadFinishReceiver);
            mUploadFinishReceiver = null;
        }
    }

    
    @Override
    public void onStop() {
        super.onStop();
        leaveTransferProgress();
    }

    
    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
        MenuItem item = menu.findItem(R.id.action_see_details);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        List<Integer> toHide = new ArrayList<Integer>();
        List<Integer> toShow = new ArrayList<Integer>();
        OCFile file = getFile();
        
        FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
        boolean downloading = downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file);
        FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
        boolean uploading = uploaderBinder != null && uploaderBinder.isUploading(mAccount, getFile());
        
        if (downloading || uploading) {
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_rename_file);
            toHide.add(R.id.action_remove_file);
            toHide.add(R.id.action_open_file_with);
            if (!downloading) {
                toHide.add(R.id.action_cancel_download);
                toShow.add(R.id.action_cancel_upload);
            } else {
                toHide.add(R.id.action_cancel_upload);
                toShow.add(R.id.action_cancel_download);
            }

        } else if (file != null && file.isDown()) {
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_cancel_download);
            toHide.add(R.id.action_cancel_upload);
            
            toShow.add(R.id.action_rename_file);
            toShow.add(R.id.action_remove_file);
            toShow.add(R.id.action_open_file_with);
            toShow.add(R.id.action_sync_file);
            
        } else if (file != null) {
            toHide.add(R.id.action_open_file_with);
            toHide.add(R.id.action_cancel_download);
            toHide.add(R.id.action_cancel_upload);
            toHide.add(R.id.action_sync_file);
            
            toShow.add(R.id.action_rename_file);
            toShow.add(R.id.action_remove_file);
            toShow.add(R.id.action_download_file);
            
        } else {
            toHide.add(R.id.action_open_file_with);
            toHide.add(R.id.action_cancel_download);
            toHide.add(R.id.action_cancel_upload);
            toHide.add(R.id.action_sync_file);
            toHide.add(R.id.action_download_file);
            toHide.add(R.id.action_rename_file);
            toHide.add(R.id.action_remove_file);
            
        }

        MenuItem item = null;
        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
            }
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                FileDisplayActivity activity = (FileDisplayActivity) getSherlockActivity();
                activity.getFileOperationsHelper().shareFileWithLink(getFile(), activity);
                return true;
            }
            case R.id.action_open_file_with: {
                FileDisplayActivity activity = (FileDisplayActivity) getSherlockActivity();
                activity.getFileOperationsHelper().openFile(getFile(), activity);
                return true;
            }
            case R.id.action_remove_file: {
                removeFile();
                return true;
            }
            case R.id.action_rename_file: {
                renameFile();
                return true;
            }
            case R.id.action_download_file: 
            case R.id.action_cancel_download:
            case R.id.action_cancel_upload:
            case R.id.action_sync_file: {
                synchronizeFile();
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdKeepInSync: {
                toggleKeepInSync();
                break;
            }
            case R.id.fdCancelBtn: {
                synchronizeFile();
                break;
            }
            default:
                Log_OC.e(TAG, "Incorrect view clicked!");
        }
    }
    
    
    private void toggleKeepInSync() {
        CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
        OCFile file = getFile();
        file.setKeepInSync(cb.isChecked());
        mStorageManager.saveFile(file);
        
        /// register the OCFile instance in the observer service to monitor local updates;
        /// if necessary, the file is download 
        Intent intent = new Intent(getActivity().getApplicationContext(),
                                   FileObserverService.class);
        intent.putExtra(FileObserverService.KEY_FILE_CMD,
                   (cb.isChecked()?
                           FileObserverService.CMD_ADD_OBSERVED_FILE:
                           FileObserverService.CMD_DEL_OBSERVED_FILE));
        intent.putExtra(FileObserverService.KEY_CMD_ARG_FILE, file);
        intent.putExtra(FileObserverService.KEY_CMD_ARG_ACCOUNT, mAccount);
        getActivity().startService(intent);
        
        if (file.keepInSync()) {
            synchronizeFile();   // force an immediate synchronization
        }
    }

    private void removeFile() {
        OCFile file = getFile();
        ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                R.string.confirmation_remove_alert,
                new String[]{file.getFileName()},
                file.isDown() ? R.string.confirmation_remove_remote_and_local : R.string.confirmation_remove_remote,
                file.isDown() ? R.string.confirmation_remove_local : -1,
                R.string.common_cancel);
        confDialog.setOnConfirmationListener(this);
        confDialog.show(getFragmentManager(), FTAG_CONFIRMATION);
    }


    private void renameFile() {
        OCFile file = getFile();
        String fileName = file.getFileName();
        int extensionStart = file.isFolder() ? -1 : fileName.lastIndexOf(".");
        int selectionEnd = (extensionStart >= 0) ? extensionStart : fileName.length();
        EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.rename_dialog_title), fileName, 0, selectionEnd, this);
        dialog.show(getFragmentManager(), "nameeditdialog");
    }

    private void synchronizeFile() {
        OCFile file = getFile();
        FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
        FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) {
            downloaderBinder.cancel(mAccount, file);
            if (file.isDown()) {
                setButtonsForDown();
            } else {
                setButtonsForRemote();
            }

        } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file)) {
            uploaderBinder.cancel(mAccount, file);
            if (!file.fileExists()) {
                // TODO make something better
                ((FileDisplayActivity)getActivity()).cleanSecondFragment();
                
            } else if (file.isDown()) {
                setButtonsForDown();
            } else {
                setButtonsForRemote();
            }
            
        } else {
            mLastRemoteOperation = new SynchronizeFileOperation(file, null, mStorageManager, mAccount, true, getActivity());
            mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
            
            // update ui 
            ((FileDisplayActivity) getActivity()).showLoadingDialog();
            
        }
    }

    @Override
    public void onConfirmation(String callerTag) {
        OCFile file = getFile();
        if (callerTag.equals(FTAG_CONFIRMATION)) {
            if (mStorageManager.getFileById(file.getFileId()) != null) {
                mLastRemoteOperation = new RemoveFileOperation( file, 
                                                                true, 
                                                                mStorageManager);
                mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
                                
                ((FileDisplayActivity) getActivity()).showLoadingDialog();
            }
        }
    }
    
    @Override
    public void onNeutral(String callerTag) {
        OCFile file = getFile();
        mStorageManager.removeFile(file, false, true);    // TODO perform in background task / new thread
        if (file.getStoragePath() != null) {
            file.setStoragePath(null);
            updateFileDetails(file, mAccount);
        }
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log_OC.d(TAG, "REMOVAL CANCELED");
    }
    
    
    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return (mLayout == R.layout.file_details_empty || getFile() == null || mAccount == null);
    }

    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        setFile(file);
        if (ocAccount != null && ( 
                mStorageManager == null || 
                (mAccount != null && !mAccount.equals(ocAccount))
           )) {
            mStorageManager = new FileDataStorageManager(ocAccount, getActivity().getApplicationContext().getContentResolver());
        }
        mAccount = ocAccount;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     * TODO Remove parameter when the transferring state of files is kept in database. 
     * 
     * TODO REFACTORING! this method called 5 times before every time the fragment is shown! 
     * 
     * @param transferring      Flag signaling if the file should be considered as downloading or uploading, 
     *                          although {@link FileDownloaderBinder#isDownloading(Account, OCFile)}  and 
     *                          {@link FileUploaderBinder#isUploading(Account, OCFile)} return false.
     *                          
     * @param refresh           If 'true', try to refresh the hold file from the database
     */
    public void updateFileDetails(boolean transferring, boolean refresh) {

        if (readyToShow()) {
            
            if (refresh && mStorageManager != null) {
                setFile(mStorageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();
            
            // set file details
            setFilename(file.getFileName());
            setFiletype(file.getMimetype());
            setFilesize(file.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(file.getCreationTimestamp());
            }
           
            setTimeModified(file.getModificationTimestamp());
            
            CheckBox cb = (CheckBox)getView().findViewById(R.id.fdKeepInSync);
            cb.setChecked(file.keepInSync());

            // configure UI for depending upon local state of the file
            //if (FileDownloader.isDownloading(mAccount, mFile.getRemotePath()) || FileUploader.isUploading(mAccount, mFile.getRemotePath())) {
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (transferring || (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) || (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file))) {
                setButtonsForTransferring();
                
            } else if (file.isDown()) {
                
                setButtonsForDown();
                
            } else {
                // TODO load default preview image; when the local file is removed, the preview remains there
                setButtonsForRemote();
            }
        }
        getView().invalidate();
    }
    
    /**
     * Checks if the fragment is ready to show details of a OCFile
     *  
     * @return  'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return (getFile() != null && mAccount != null && mLayout == R.layout.file_details_fragment);        
    }


    /**
     * Updates the filename in view
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null)
            tv.setText(filename);
    }

    /**
     * Updates the MIME type in view
     * @param mimetype to set
     */
    private void setFiletype(String mimetype) {
        TextView tv = (TextView) getView().findViewById(R.id.fdType);
        if (tv != null) {
            String printableMimetype = DisplayUtils.convertMIMEtoPrettyPrint(mimetype);;        
            tv.setText(printableMimetype);
        }
        ImageView iv = (ImageView) getView().findViewById(R.id.fdIcon);
        if (iv != null) {
            iv.setImageResource(DisplayUtils.getResourceId(mimetype));
        }
    }

    /**
     * Updates the file size in view
     * @param filesize in bytes to set
     */
    private void setFilesize(long filesize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
        if (tv != null)
            tv.setText(DisplayUtils.bytesToHumanReadable(filesize));
    }
    
    /**
     * Updates the time that the file was created in view
     * @param milliseconds Unix time to set
     */
    private void setTimeCreated(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdCreated);
        TextView tvLabel = (TextView) getView().findViewById(R.id.fdCreatedLabel);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
            tv.setVisibility(View.VISIBLE);
            tvLabel.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the time that the file was last modified
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds){
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if(tv != null){
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }
    
    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // let's protect the user from himself ;)
            getView().findViewById(R.id.fdKeepInSync).setEnabled(false);
            
            // show the progress bar for the transfer
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.VISIBLE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, getFile())) {
                progressText.setText(R.string.downloader_download_in_progress_ticker);
            } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, getFile())) {
                progressText.setText(R.string.uploader_upload_in_progress_ticker);
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available 
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available 
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView)getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }
    

    /**
     * In ownCloud 3.X.X and 4.X.X there is a bug that SabreDAV does not return
     * the time that the file was created. There is a chance that this will
     * be fixed in future versions. Use this method to check if this version of
     * ownCloud has this fix.
     * @return True, if ownCloud the ownCloud version is supporting creation time
     */
    private boolean ocVersionSupportsTimeCreated(){
        /*if(mAccount != null){
            AccountManager accManager = (AccountManager) getActivity().getSystemService(Context.ACCOUNT_SERVICE);
            OwnCloudVersion ocVersion = new OwnCloudVersion(accManager
                    .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
            if(ocVersion.compareTo(new OwnCloudVersion(0x030000)) < 0) {
                return true;
            }
        }*/
        return false;
    }
    

    /**
     * Once the file upload has finished -> update view
     * 
     * Being notified about the finish of an upload is necessary for the next sequence:
     *   1. Upload a big file.
     *   2. Force a synchronization; if it finished before the upload, the file in transfer will be included in the local database and in the file list
     *      of its containing folder; the the server includes it in the PROPFIND requests although it's not fully upload. 
     *   3. Click the file in the list to see its details.
     *   4. Wait for the upload finishes; at this moment, the details view must be refreshed to enable the action buttons.
     */
    private class UploadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);

            if (!isEmpty() && accountName.equals(mAccount.name)) {
                boolean uploadWasFine = intent.getBooleanExtra(FileUploader.EXTRA_UPLOAD_RESULT, false);
                String uploadRemotePath = intent.getStringExtra(FileUploader.EXTRA_REMOTE_PATH);
                boolean renamedInUpload = getFile().getRemotePath().equals(intent.getStringExtra(FileUploader.EXTRA_OLD_REMOTE_PATH));
                if (getFile().getRemotePath().equals(uploadRemotePath) ||
                    renamedInUpload) {
                    if (uploadWasFine) {
                        setFile(mStorageManager.getFileByPath(uploadRemotePath));
                    }
                    if (renamedInUpload) {
                        String newName = (new File(uploadRemotePath)).getName();
                        Toast msg = Toast.makeText(getActivity().getApplicationContext(), String.format(getString(R.string.filedetails_renamed_in_upload_msg), newName), Toast.LENGTH_LONG);
                        msg.show();
                    }
                    getSherlockActivity().removeStickyBroadcast(intent);    // not the best place to do this; a small refactorization of BroadcastReceivers should be done
                    
                    updateFileDetails(false, false);    // it updates the buttons; must be called although !uploadWasFine; interrupted uploads still leave an incomplete file in the server
                   
                    // Force the preview if the file is an image
                    if (uploadWasFine && PreviewImageFragment.canBePreviewed(getFile())) {
                        ((FileDisplayActivity) mContainerActivity).startImagePreview(getFile());
                    } 
                }
            }
        }
    }
    

    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log_OC.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            mLastRemoteOperation = new RenameFileOperation( getFile(), 
                                                            mAccount, 
                                                            newFilename, 
                                                            new FileDataStorageManager(mAccount, getActivity().getContentResolver()));
            mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
            ((FileDisplayActivity) getActivity()).showLoadingDialog();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation.equals(mLastRemoteOperation)) {
            if (operation instanceof RemoveFileOperation) {
                onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
                
            } else if (operation instanceof RenameFileOperation) {
                onRenameFileOperationFinish((RenameFileOperation)operation, result);
                
            } else if (operation instanceof SynchronizeFileOperation) {
                onSynchronizeFileOperationFinish((SynchronizeFileOperation)operation, result);
            }
        }
    }
    
    
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        ((FileDisplayActivity) getActivity()).dismissLoadingDialog();
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            ((FileDisplayActivity)getActivity()).cleanSecondFragment();

        } else {
            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                // TODO show the SSL warning dialog
            }
        }
    }
    
    private void onRenameFileOperationFinish(RenameFileOperation operation, RemoteOperationResult result) {
        ((FileDisplayActivity) getActivity()).dismissLoadingDialog();
        
        if (result.isSuccess()) {
            updateFileDetails(((RenameFileOperation)operation).getFile(), mAccount);
            mContainerActivity.onFileStateChanged();
            
        } else {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_local_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                // TODO throw again the new rename dialog
            } if (result.getCode().equals(ResultCode.INVALID_CHARACTER_IN_NAME)) {
                Toast msg = Toast.makeText(getActivity(), R.string.filename_forbidden_characters, Toast.LENGTH_LONG);
                msg.show();
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_server_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (result.isSslRecoverableException()) {
                    // TODO show the SSL warning dialog
                }
            }
        }
    }
    
    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation, RemoteOperationResult result) {
        ((FileDisplayActivity) getActivity()).dismissLoadingDialog();
        OCFile file = getFile();
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(getActivity(), ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
                startActivity(i);
                
            } 
            
            if (file.isDown()) {
                setButtonsForDown();
                
            } else {
                setButtonsForRemote();
            }
            
        } else {
            if (operation.transferWasRequested()) {
                setButtonsForTransferring();
                mContainerActivity.onFileStateChanged();    // this is not working; FileDownloader won't do NOTHING at all until this method finishes, so 
                                                            // checking the service to see if the file is downloading results in FALSE
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.sync_file_nothing_to_do_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (file.isDown()) {
                    setButtonsForDown();
                    
                } else {
                    setButtonsForRemote();
                }
            }
        }
    }
    

    public void listenForTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        }
    }
    
    
    public void leaveTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        }
    }


    
    /**
     * Helper class responsible for updating the progress bar shown for file uploading or downloading  
     * 
     * @author David A. Velasco
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent = 0;
        WeakReference<ProgressBar> mProgressBar = null;
        
        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<ProgressBar>(progressBar);
        }
        
        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String filename) {
            int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
            if (percent != mLastPercent) {
                ProgressBar pb = mProgressBar.get();
                if (pb != null) {
                    pb.setProgress(percent);
                    pb.postInvalidate();
                }
            }
            mLastPercent = percent;
        }

    };

}
