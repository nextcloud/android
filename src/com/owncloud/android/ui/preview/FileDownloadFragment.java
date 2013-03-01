/* ownCloud Android client application
 * 
 *   Copyright (C) 2012-2013  ownCloud Inc.
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
package com.owncloud.android.ui.preview;

import java.lang.ref.WeakReference;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.ui.fragment.FileFragment;

import com.owncloud.android.R;

import eu.alefzero.webdav.OnDatatransferProgressListener;

/**
 * This Fragment is used to monitor the progress of a file downloading.
 * 
 * @author David A. Velasco
 */
public class FileDownloadFragment extends SherlockFragment implements OnClickListener, FileFragment {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_ERROR = "ERROR";

    private FileFragment.ContainerActivity mContainerActivity;
    
    private View mView;
    private OCFile mFile;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    
    public ProgressListener mProgressListener;
    private boolean mListening;
    
    private static final String TAG = FileDownloadFragment.class.getSimpleName();
    
    private boolean mIgnoreFirstSavedState;
    private boolean mError;
    

    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstantiate a fragment automatically. 
     */
    public FileDownloadFragment() {
        mFile = null;
        mAccount = null;
        mStorageManager = null;
        mProgressListener = null;
        mListening = false;
        mIgnoreFirstSavedState = false;
        mError = false;
    }
    
    
    /**
     * Creates a details fragment.
     * 
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     * 
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter}; TODO better solution 
     */
    public FileDownloadFragment(OCFile fileToDetail, Account ocAccount, boolean ignoreFirstSavedState) {
        mFile = fileToDetail;
        mAccount = ocAccount;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
        mProgressListener = null;
        mListening = false;
        mIgnoreFirstSavedState = ignoreFirstSavedState;
        mError = false;
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        
        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                mFile = savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_FILE);
                mAccount = savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_ACCOUNT);
                mError = savedInstanceState.getBoolean(FileDownloadFragment.EXTRA_ERROR);
            } else {
                mIgnoreFirstSavedState = false;
            }
        }
        
        View view = null;
        view = inflater.inflate(R.layout.file_download_fragment, container, false);
        mView = view;
        
        ProgressBar progressBar = (ProgressBar)mView.findViewById(R.id.progressBar);
        mProgressListener = new ProgressListener(progressBar);
        
        ((Button)mView.findViewById(R.id.cancelBtn)).setOnClickListener(this);
        
        if (mError) {
            setButtonsForRemote();
        } else {
            setButtonsForTransferring();
        }
        
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
            throw new ClassCastException(activity.toString() + " must implement " + FileFragment.ContainerActivity.class.getSimpleName());
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
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDownloadFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FileDownloadFragment.EXTRA_ACCOUNT, mAccount);
        outState.putBoolean(FileDownloadFragment.EXTRA_ERROR, mError);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    
    @Override
    public void onStop() {
        super.onStop();
        leaveTransferProgress();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    
    @Override
    public View getView() {
        if (!mListening) {
            listenForTransferProgress();
        }
        return super.getView() == null ? mView : super.getView();
    }

    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelBtn: {
                FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
                if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile)) {
                    downloaderBinder.cancel(mAccount, mFile);
                    getActivity().finish(); // :)
                    /*
                    leaveTransferProgress();
                    if (mFile.isDown()) {
                        setButtonsForDown();
                    } else {
                        setButtonsForRemote();
                    }
                    */
                }
                break;
            }
            default:
                Log.e(TAG, "Incorrect view clicked!");
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public OCFile getFile(){
        return mFile;
    }
    
    
    /**
     * Updates the view depending upon the state of the downloading file.
     * 
     * @param   transferring    When true, the view must be updated assuming that the holded file is 
     *                          downloading, no matter what the downloaderBinder says.
     */
    public void updateView(boolean transferring) {
        // configure UI for depending upon local state of the file
        FileDownloaderBinder downloaderBinder = (mContainerActivity == null) ? null : mContainerActivity.getFileDownloaderBinder();
        if (transferring || (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile))) {
            setButtonsForTransferring();
            
        } else if (mFile.isDown()) {
            
            setButtonsForDown();
            
        } else {
            setButtonsForRemote();
        }
        getView().invalidate();
        
    }


    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        getView().findViewById(R.id.cancelBtn).setVisibility(View.VISIBLE);
    
        // show the progress bar for the transfer
        getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        TextView progressText = (TextView)getView().findViewById(R.id.progressText);
        progressText.setText(R.string.downloader_download_in_progress_ticker);
        progressText.setVisibility(View.VISIBLE);
                
        // hides the error icon
        getView().findViewById(R.id.errorText).setVisibility(View.GONE);
        getView().findViewById(R.id.error_image).setVisibility(View.GONE);
    }
    

    /**
     * Enables or disables buttons for a file locally available 
     */
    private void setButtonsForDown() {
        getView().findViewById(R.id.cancelBtn).setVisibility(View.GONE);
    
        // hides the progress bar
        getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        
        // updates the text message
        TextView progressText = (TextView)getView().findViewById(R.id.progressText);
        progressText.setText(R.string.common_loading);
        progressText.setVisibility(View.VISIBLE);
        
        // hides the error icon
        getView().findViewById(R.id.errorText).setVisibility(View.GONE);
        getView().findViewById(R.id.error_image).setVisibility(View.GONE);
    }

    
    /**
     * Enables or disables buttons for a file not locally available 
     * 
     * Currently, this is only used when a download was failed
     */
    private void setButtonsForRemote() {
        getView().findViewById(R.id.cancelBtn).setVisibility(View.GONE);
        
        // hides the progress bar and message
        getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        getView().findViewById(R.id.progressText).setVisibility(View.GONE);

        // shows the error icon and message
        getView().findViewById(R.id.errorText).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.error_image).setVisibility(View.VISIBLE);
    }
    

    public void listenForTransferProgress() {
        if (mProgressListener != null && !mListening) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().addDatatransferProgressListener(mProgressListener, mAccount, mFile);
                mListening = true;
                setButtonsForTransferring();
            }
        }
    }
    
    
    public void leaveTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().removeDatatransferProgressListener(mProgressListener, mAccount, mFile);
                mListening = false;
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
        public void onTransferProgress(long progressRate) {
            // old method, nothing here
        };

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

    }


    public void setError(boolean error) {
        mError = error;
    };
    


}
