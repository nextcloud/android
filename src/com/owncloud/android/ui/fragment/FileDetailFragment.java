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

import org.apache.commons.httpclient.Credentials;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.DisplayUtils;
import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileObserverService;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.network.BearerCredentials;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavUtils;

/**
 * This Fragment is used to display the details about a file.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileDetailFragment extends SherlockFragment implements
        OnClickListener, ConfirmationDialogFragment.ConfirmationDialogFragmentListener, OnRemoteOperationListener, EditNameDialogListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";

    private FileDetailFragment.ContainerActivity mContainerActivity;
    
    private int mLayout;
    private View mView;
    private OCFile mFile;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private ImageView mPreview;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    
    private Handler mHandler;
    private RemoteOperation mLastRemoteOperation;
    private DialogFragment mCurrentDialog;

    private static final String TAG = FileDetailFragment.class.getSimpleName();
    public static final String FTAG = "FileDetails"; 
    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";

    
    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDetailFragment() {
        mFile = null;
        mAccount = null;
        mStorageManager = null;
        mLayout = R.layout.file_details_empty;
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
        mFile = fileToDetail;
        mAccount = ocAccount;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
        mLayout = R.layout.file_details_empty;
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_ACCOUNT);
        }
        
        if(mFile != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }
        
        View view = null;
        view = inflater.inflate(mLayout, container, false);
        mView = view;
        
        if (mLayout == R.layout.file_details_fragment) {
            mView.findViewById(R.id.fdKeepInSync).setOnClickListener(this);
            mView.findViewById(R.id.fdRenameBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdDownloadBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdOpenBtn).setOnClickListener(this);
            mView.findViewById(R.id.fdRemoveBtn).setOnClickListener(this);
            //mView.findViewById(R.id.fdShareBtn).setOnClickListener(this);
            mPreview = (ImageView)mView.findViewById(R.id.fdPreview);
        }
        
        updateFileDetails(false);
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
            mStorageManager = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());;
        }
    }
        

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.i(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        Log.i(getClass().toString(), "onSaveInstanceState() end");
    }

    
    @Override
    public void onResume() {
        super.onResume();
        
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(
                FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mDownloadFinishReceiver, filter);
        
        mUploadFinishReceiver = new UploadFinishReceiver();
        filter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mUploadFinishReceiver, filter);
        
        mPreview = (ImageView)mView.findViewById(R.id.fdPreview);
    }

    @Override
    public void onPause() {
        super.onPause();
        
        getActivity().unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
        
        getActivity().unregisterReceiver(mUploadFinishReceiver);
        mUploadFinishReceiver = null;
        
        if (mPreview != null) {
            mPreview = null;
        }
    }

    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }

    
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdDownloadBtn: {
                FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
                FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
                if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile)) {
                    downloaderBinder.cancel(mAccount, mFile);
                    if (mFile.isDown()) {
                        setButtonsForDown();
                    } else {
                        setButtonsForRemote();
                    }

                } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile)) {
                    uploaderBinder.cancel(mAccount, mFile);
                    if (!mFile.fileExists()) {
                        // TODO make something better
                        if (getActivity() instanceof FileDisplayActivity) {
                            // double pane
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null), FTAG); // empty FileDetailFragment
                            transaction.commit();
                            mContainerActivity.onFileStateChanged();
                        } else {
                            getActivity().finish();
                        }
                        
                    } else if (mFile.isDown()) {
                        setButtonsForDown();
                    } else {
                        setButtonsForRemote();
                    }
                    
                } else {
                    mLastRemoteOperation = new SynchronizeFileOperation(mFile, null, mStorageManager, mAccount, true, false, getActivity());
                    mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
                
                    // update ui 
                    boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                    getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                    setButtonsForTransferring(); // disable button immediately, although the synchronization does not result in a file transference
                    
                }
                break;
            }
            case R.id.fdKeepInSync: {
                CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
                mFile.setKeepInSync(cb.isChecked());
                mStorageManager.saveFile(mFile);
                
                /// register the OCFile instance in the observer service to monitor local updates;
                /// if necessary, the file is download 
                Intent intent = new Intent(getActivity().getApplicationContext(),
                                           FileObserverService.class);
                intent.putExtra(FileObserverService.KEY_FILE_CMD,
                           (cb.isChecked()?
                                   FileObserverService.CMD_ADD_OBSERVED_FILE:
                                   FileObserverService.CMD_DEL_OBSERVED_FILE));
                intent.putExtra(FileObserverService.KEY_CMD_ARG_FILE, mFile);
                intent.putExtra(FileObserverService.KEY_CMD_ARG_ACCOUNT, mAccount);
                Log.e(TAG, "starting observer service");
                getActivity().startService(intent);
                
                if (mFile.keepInSync()) {
                    onClick(getView().findViewById(R.id.fdDownloadBtn));    // force an immediate synchronization
                }
                break;
            }
            case R.id.fdRenameBtn: {
                EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.rename_dialog_title), mFile.getFileName(), this);
                dialog.show(getFragmentManager(), "nameeditdialog");
                break;
            }   
            case R.id.fdRemoveBtn: {
                ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                        R.string.confirmation_remove_alert,
                        new String[]{mFile.getFileName()},
                        mFile.isDown() ? R.string.confirmation_remove_remote_and_local : R.string.confirmation_remove_remote,
                        mFile.isDown() ? R.string.confirmation_remove_local : -1,
                        R.string.common_cancel);
                confDialog.setOnConfirmationListener(this);
                mCurrentDialog = confDialog;
                mCurrentDialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                break;
            }
            case R.id.fdOpenBtn: {
                String storagePath = mFile.getStoragePath();
                String encodedStoragePath = WebdavUtils.encodePath(storagePath);
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mFile.getMimetype());
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(i);
                    
                } catch (Throwable t) {
                    Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mFile.getMimetype());
                    boolean toastIt = true; 
                    String mimeType = "";
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                        if (mimeType == null || !mimeType.equals(mFile.getMimetype())) {
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
                            Toast.makeText(getActivity(), "There is no application to handle file " + mFile.getFileName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                }
                break;
            }
            default:
                Log.e(TAG, "Incorrect view clicked!");
        }
        
        /* else if (v.getId() == R.id.fdShareBtn) {
            Thread t = new Thread(new ShareRunnable(mFile.getRemotePath()));
            t.start();
        }*/
    }
    
    
    @Override
    public void onConfirmation(String callerTag) {
        if (callerTag.equals(FTAG_CONFIRMATION)) {
            if (mStorageManager.getFileById(mFile.getFileId()) != null) {
                mLastRemoteOperation = new RemoveFileOperation( mFile, 
                                                                true, 
                                                                mStorageManager);
                mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
                boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
            }
        }
        mCurrentDialog.dismiss();
        mCurrentDialog = null;
    }
    
    @Override
    public void onNeutral(String callerTag) {
        File f = null;
        if (mFile.isDown() && (f = new File(mFile.getStoragePath())).exists()) {
            f.delete();
            mFile.setStoragePath(null);
            mStorageManager.saveFile(mFile);
            updateFileDetails(mFile, mAccount);
        }
        mCurrentDialog.dismiss();
        mCurrentDialog = null;
    }
    
    @Override
    public void onCancel(String callerTag) {
        Log.d(TAG, "REMOVAL CANCELED");
        mCurrentDialog.dismiss();
        mCurrentDialog = null;
    }
    
    
    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     * 
     * @return  True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return (mLayout == R.layout.file_details_empty || mFile == null || mAccount == null);
    }

    
    /**
     * Can be used to get the file that is currently being displayed.
     * @return The file on the screen.
     */
    public OCFile getDisplayedFile(){
        return mFile;
    }
    
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        mFile = file;
        if (ocAccount != null && ( 
                mStorageManager == null || 
                (mAccount != null && !mAccount.equals(ocAccount))
           )) {
            mStorageManager = new FileDataStorageManager(ocAccount, getActivity().getApplicationContext().getContentResolver());
        }
        mAccount = ocAccount;
        updateFileDetails(false);
    }
    

    /**
     * Updates the view with all relevant details about that file.
     *
     * TODO Remove parameter when the transferring state of files is kept in database. 
     * 
     * @param transferring      Flag signaling if the file should be considered as downloading or uploading, 
     *                          although {@link FileDownloaderBinder#isDownloading(Account, OCFile)}  and 
     *                          {@link FileUploaderBinder#isUploading(Account, OCFile)} return false.
     * 
     */
    public void updateFileDetails(boolean transferring) {

        if (mFile != null && mAccount != null && mLayout == R.layout.file_details_fragment) {
            
            // set file details
            setFilename(mFile.getFileName());
            setFiletype(mFile.getMimetype());
            setFilesize(mFile.getFileLength());
            if(ocVersionSupportsTimeCreated()){
                setTimeCreated(mFile.getCreationTimestamp());
            }
           
            setTimeModified(mFile.getModificationTimestamp());
            
            CheckBox cb = (CheckBox)getView().findViewById(R.id.fdKeepInSync);
            cb.setChecked(mFile.keepInSync());

            // configure UI for depending upon local state of the file
            //if (FileDownloader.isDownloading(mAccount, mFile.getRemotePath()) || FileUploader.isUploading(mAccount, mFile.getRemotePath())) {
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (transferring || (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile)) || (uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile))) {
                setButtonsForTransferring();
                
            } else if (mFile.isDown()) {
                // Update preview
                if (mFile.getMimetype().startsWith("image/")) {
                    BitmapLoader bl = new BitmapLoader();
                    bl.execute(new String[]{mFile.getStoragePath()});
                }
                
                setButtonsForDown();
                
            } else {
                // TODO load default preview image; when the local file is removed, the preview remains there
                setButtonsForRemote();
            }
        }
        getView().invalidate();
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
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            downloadButton.setText(R.string.common_cancel);
            //downloadButton.setEnabled(false);
        
            // let's protect the user from himself ;)
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(false);
            getView().findViewById(R.id.fdKeepInSync).setEnabled(false);
        }
    }
    
    /**
     * Enables or disables buttons for a file locally available 
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            downloadButton.setText(R.string.filedetails_sync_file);
        
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(true);
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available 
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            Button downloadButton = (Button) getView().findViewById(R.id.fdDownloadBtn);
            downloadButton.setText(R.string.filedetails_download);
            
            ((Button) getView().findViewById(R.id.fdOpenBtn)).setEnabled(false);
            ((Button) getView().findViewById(R.id.fdRenameBtn)).setEnabled(true);
            ((Button) getView().findViewById(R.id.fdRemoveBtn)).setEnabled(true);
            getView().findViewById(R.id.fdKeepInSync).setEnabled(true);
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
     * Interface to implement by any Activity that includes some instance of FileDetailFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity extends TransferServiceGetter {

        /**
         * Callback method invoked when the detail fragment wants to notice its container 
         * activity about a relevant state the file shown by the fragment.
         * 
         * Added to notify to FileDisplayActivity about the need of refresh the files list. 
         * 
         * Currently called when:
         *  - a download is started;
         *  - a rename is completed;
         *  - a deletion is completed;
         *  - the 'inSync' flag is changed;
         */
        public void onFileStateChanged();
        
    }
    

    /**
     * Once the file download has finished -> update view
     * @author Bartek Przybylski
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);

            if (!isEmpty() && accountName.equals(mAccount.name)) {
                boolean downloadWasFine = intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false);
                String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
                if (mFile.getRemotePath().equals(downloadedRemotePath)) {
                    if (downloadWasFine) {
                        mFile = mStorageManager.getFileByPath(downloadedRemotePath);
                    }
                    updateFileDetails(false);    // it updates the buttons; must be called although !downloadWasFine
                }
            }
        }
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
                boolean renamedInUpload = mFile.getRemotePath().equals(intent.getStringExtra(FileUploader.EXTRA_OLD_REMOTE_PATH));
                if (mFile.getRemotePath().equals(uploadRemotePath) ||
                    renamedInUpload) {
                    if (uploadWasFine) {
                       mFile = mStorageManager.getFileByPath(uploadRemotePath);
                    }
                    if (renamedInUpload) {
                        String newName = (new File(uploadRemotePath)).getName();
                        Toast msg = Toast.makeText(getActivity().getApplicationContext(), String.format(getString(R.string.filedetails_renamed_in_upload_msg), newName), Toast.LENGTH_LONG);
                        msg.show();
                    }
                    getSherlockActivity().removeStickyBroadcast(intent);    // not the best place to do this; a small refactorization of BroadcastReceivers should be done
                    updateFileDetails(false);    // it updates the buttons; must be called although !uploadWasFine; interrupted uploads still leave an incomplete file in the server
                }
            }
        }
    }
    

    // this is a temporary class for sharing purposes, it need to be replaced in transfer service
    /*
    @SuppressWarnings("unused")
    private class ShareRunnable implements Runnable {
        private String mPath;

        public ShareRunnable(String path) {
            mPath = path;
        }
        
        public void run() {
            AccountManager am = AccountManager.get(getActivity());
            Account account = AccountUtils.getCurrentOwnCloudAccount(getActivity());
            OwnCloudVersion ocv = new OwnCloudVersion(am.getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            String url = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + AccountUtils.getWebdavPath(ocv);

            Log.d("share", "sharing for version " + ocv.toString());

            if (ocv.compareTo(new OwnCloudVersion(0x040000)) >= 0) {
                String APPS_PATH = "/apps/files_sharing/";
                String SHARE_PATH = "ajax/share.php";

                String SHARED_PATH = "/apps/files_sharing/get.php?token=";
                
                final String WEBDAV_SCRIPT = "webdav.php";
                final String WEBDAV_FILES_LOCATION = "/files/";
                
                WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(account, getActivity().getApplicationContext());
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setMaxConnectionsPerHost(wc.getHostConfiguration(), 5);

                //wc.getParams().setParameter("http.protocol.single-cookie-header", true);
                //wc.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

                PostMethod post = new PostMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + APPS_PATH + SHARE_PATH);

                post.addRequestHeader("Content-type","application/x-www-form-urlencoded; charset=UTF-8" );
                post.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                Log.d("share", mPath+"");
                formparams.add(new BasicNameValuePair("sources",mPath));
                formparams.add(new BasicNameValuePair("uid_shared_with", "public"));
                formparams.add(new BasicNameValuePair("permissions", "0"));
                post.setRequestEntity(new StringRequestEntity(URLEncodedUtils.format(formparams, HTTP.UTF_8)));

                int status;
                try {
                    PropFindMethod find = new PropFindMethod(url+"/");
                    find.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    Log.d("sharer", ""+ url+"/");
                    
                    for (org.apache.commons.httpclient.Header a : find.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    
                    int status2 = wc.executeMethod(find);

                    Log.d("sharer", "propstatus "+status2);
                    
                    GetMethod get = new GetMethod(am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + "/");
                    get.addRequestHeader("Referer", am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL));
                    
                    status2 = wc.executeMethod(get);

                    Log.d("sharer", "getstatus "+status2);
                    Log.d("sharer", "" + get.getResponseBodyAsString());
                    
                    for (org.apache.commons.httpclient.Header a : get.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }

                    status = wc.executeMethod(post);
                    for (org.apache.commons.httpclient.Header a : post.getRequestHeaders()) {
                        Log.d("sharer-h", a.getName() + ":"+a.getValue());
                    }
                    for (org.apache.commons.httpclient.Header a : post.getResponseHeaders()) {
                        Log.d("sharer", a.getName() + ":"+a.getValue());
                    }
                    String resp = post.getResponseBodyAsString();
                    Log.d("share", ""+post.getURI().toString());
                    Log.d("share", "returned status " + status);
                    Log.d("share", " " +resp);
                    
                    if(status != HttpStatus.SC_OK ||resp == null || resp.equals("") || resp.startsWith("false")) {
                        return;
                     }

                    JSONObject jsonObject = new JSONObject (resp);
                    String jsonStatus = jsonObject.getString("status");
                    if(!jsonStatus.equals("success")) throw new Exception("Error while sharing file status != success");
                    
                    String token = jsonObject.getString("data");
                    String uri = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL) + SHARED_PATH + token; 
                    Log.d("Actions:shareFile ok", "url: " + uri);   
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
            } else if (ocv.compareTo(new OwnCloudVersion(0x030000)) >= 0) {
                
            }
        }
    }
    */
    
    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            mLastRemoteOperation = new RenameFileOperation( mFile, 
                                                            mAccount, 
                                                            newFilename, 
                                                            new FileDataStorageManager(mAccount, getActivity().getContentResolver()));
            mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
            boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
            getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        }
    }
    
    
    class BitmapLoader extends AsyncTask<String, Void, Bitmap> {
        @SuppressLint({ "NewApi", "NewApi", "NewApi" }) // to avoid Lint errors since Android SDK r20
		@Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            if (params.length != 1) return result;
            String storagePath = params[0];
            try {

                BitmapFactory.Options options = new Options();
                options.inScaled = true;
                options.inPurgeable = true;
                options.inJustDecodeBounds = true;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                    options.inPreferQualityOverSpeed = false;
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    options.inMutable = false;
                }

                result = BitmapFactory.decodeFile(storagePath, options);
                options.inJustDecodeBounds = false;

                int width = options.outWidth;
                int height = options.outHeight;
                int scale = 1;
                if (width >= 2048 || height >= 2048) {
                    scale = (int) Math.ceil((Math.ceil(Math.max(height, width) / 2048.)));
                    options.inSampleSize = scale;
                }
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                int screenwidth;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                    display.getSize(size);
                    screenwidth = size.x;
                } else {
                    screenwidth = display.getWidth();
                }

                Log.e("ASD", "W " + width + " SW " + screenwidth);

                if (width > screenwidth) {
                    scale = (int) Math.ceil((float)width / screenwidth);
                    options.inSampleSize = scale;
                }

                result = BitmapFactory.decodeFile(storagePath, options);

                Log.e("ASD", "W " + options.outWidth + " SW " + options.outHeight);

            } catch (OutOfMemoryError e) {
                result = null;
                Log.e(TAG, "Out of memory occured for file with size " + storagePath);
                
            } catch (NoSuchFieldError e) {
                result = null;
                Log.e(TAG, "Error from access to unexisting field despite protection " + storagePath);
                
            } catch (Throwable t) {
                result = null;
                Log.e(TAG, "Unexpected error while creating image preview " + storagePath, t);
            }
            return result;
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && mPreview != null) {
                mPreview.setImageBitmap(result);
            }
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
                
        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation)operation, result);
                
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation)operation, result);
        }
    }
    
    
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            if (inDisplayActivity) {
                // double pane
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null)); // empty FileDetailFragment
                transaction.commit();
                mContainerActivity.onFileStateChanged();
            } else {
                getActivity().finish();
            }
                
        } else {
            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                // TODO show the SSL warning dialog
            }
        }
    }
    
    private void onRenameFileOperationFinish(RenameFileOperation operation, RemoteOperationResult result) {
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        
        if (result.isSuccess()) {
            updateFileDetails(((RenameFileOperation)operation).getFile(), mAccount);
            mContainerActivity.onFileStateChanged();
            
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
    
    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation, RemoteOperationResult result) {
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);

        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(getActivity(), ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, mFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
                startActivity(i);
                
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.sync_file_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
            }
            
            if (mFile.isDown()) {
                setButtonsForDown();
                
            } else {
                setButtonsForRemote();
            }
            
        } else {
            if (operation.transferWasRequested()) {
                mContainerActivity.onFileStateChanged();    // this is not working; FileDownloader won't do NOTHING at all until this method finishes, so 
                                                            // checking the service to see if the file is downloading results in FALSE
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.sync_file_nothing_to_do_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (mFile.isDown()) {
                    setButtonsForDown();
                    
                } else {
                    setButtonsForRemote();
                }
            }
        }
    }

}
