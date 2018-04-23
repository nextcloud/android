/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015  ownCloud Inc.
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
package com.owncloud.android.ui.preview;

import android.accounts.Account;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;


/**
 * This Fragment is used to monitor the progress of a file downloading.
 */
public class FileDownloadFragment extends FileFragment implements OnClickListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_ERROR = "ERROR";
    
    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_ACCOUNT = "ACCOUNT" ;

    private View mView;
    private Account mAccount;

    public ProgressListener mProgressListener;
    private boolean mListening;

    private static final String TAG = FileDownloadFragment.class.getSimpleName();

    private boolean mIgnoreFirstSavedState;
    private boolean mError;


    /**
     * Public factory method to create a new fragment that shows the progress of a file download.
     *
     * Android strongly recommends keep the empty constructor of fragments as the only public constructor, and
     * use {@link #setArguments(Bundle)} to set the needed arguments.
     *
     * This method hides to client objects the need of doing the construction in two steps.
     *
     * When 'file' is null creates a dummy layout (useful when a file wasn't tapped before).
     *
     * @param file                      An {@link OCFile} to show in the fragment
     * @param account                   An OC account; needed to start downloads
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter}
     *                                  TODO better solution
     */
    public static Fragment newInstance(OCFile file, Account account, boolean ignoreFirstSavedState) {
        FileDownloadFragment frag = new FileDownloadFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        args.putBoolean(ARG_IGNORE_FIRST, ignoreFirstSavedState);
        frag.setArguments(args);
        return frag;
    }


    /**
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to
     * reinstantiate a fragment automatically.
     */
    public FileDownloadFragment() {
        super();
        mAccount = null;
        mProgressListener = null;
        mListening = false;
        mIgnoreFirstSavedState = false;
        mError = false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile(args.getParcelable(ARG_FILE));
            // TODO better in super, but needs to check ALL the class extending FileFragment; not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        mAccount = args.getParcelable(ARG_ACCOUNT);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null) {
            if (!mIgnoreFirstSavedState) {
                setFile(savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_FILE));
                mAccount = savedInstanceState.getParcelable(FileDownloadFragment.EXTRA_ACCOUNT);
                mError = savedInstanceState.getBoolean(FileDownloadFragment.EXTRA_ERROR);
            }
            else {
                mIgnoreFirstSavedState = false;
            }
        }

        mView = inflater.inflate(R.layout.file_download_fragment, container, false);

        ProgressBar progressBar = mView.findViewById(R.id.progressBar);
        ThemeUtils.colorHorizontalProgressBar(progressBar, ThemeUtils.primaryAccentColor(getContext()));
        mProgressListener = new ProgressListener(progressBar);

        (mView.findViewById(R.id.cancelBtn)).setOnClickListener(this);
        
        (mView.findViewById(R.id.fileDownloadLL)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PreviewImageActivity) getActivity()).toggleFullScreen();
            }
        });

        if (mError) {
            setButtonsForRemote();
        }
        else {
            setButtonsForTransferring();
        }

        return mView;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDownloadFragment.EXTRA_FILE, getFile());
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
        leaveTransferProgress();
        super.onStop();
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
                mContainerActivity.getFileOperationsHelper().cancelTransference(getFile());
                getActivity().finish();
                break;
            }
            default:
                Log_OC.e(TAG, "Incorrect view clicked!");
                break;
        }
    }


    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        getView().findViewById(R.id.cancelBtn).setVisibility(View.VISIBLE);

        // show the progress bar for the transfer
        getView().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        TextView progressText = getView().findViewById(R.id.progressText);
        progressText.setText(R.string.downloader_download_in_progress_ticker);
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
        if (mProgressListener != null && !mListening && mContainerActivity.getFileDownloaderBinder() != null) {
            mContainerActivity.getFileDownloaderBinder().addDatatransferProgressListener(
                    mProgressListener, mAccount, getFile()
            );
            mListening = true;
            setButtonsForTransferring();
        }
    }


    public void leaveTransferProgress() {
        if (mProgressListener != null && mContainerActivity.getFileDownloaderBinder() != null) {
            mContainerActivity.getFileDownloaderBinder().removeDatatransferProgressListener(
                    mProgressListener, mAccount, getFile()
            );
            mListening = false;
        }
    }


    /**
     * Helper class responsible for updating the progress bar shown for file uploading or downloading
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent = 0;
        WeakReference<ProgressBar> mProgressBar = null;

        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<>(progressBar);
        }

        @Override
        public void onTransferProgress(
                long progressRate, long totalTransferredSoFar, long totalToTransfer, String filename
        ) {
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
    }
}
