/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * This Fragment is used to monitor the progress of a file downloading.
 */
public class FileDownloadFragment extends FileFragment implements OnClickListener, Injectable {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_USER = "USER";
    private static final String EXTRA_ERROR = "ERROR";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_IGNORE_FIRST = "IGNORE_FIRST";
    private static final String ARG_USER = "USER";

    private View mView;
    private User user;

    @Inject ViewThemeUtils viewThemeUtils;
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
     * @param user                      Nextcloud user; needed to start downloads
     * @param ignoreFirstSavedState     Flag to work around an unexpected behaviour of {@link FragmentStatePagerAdapter}
     *                                  TODO better solution
     */
    public static Fragment newInstance(OCFile file, User user, boolean ignoreFirstSavedState) {
        FileDownloadFragment frag = new FileDownloadFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_USER, user);
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
        mProgressListener = null;
        mListening = false;
        mIgnoreFirstSavedState = false;
        mError = false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setFile(BundleExtensionsKt.getParcelableArgument(args, ARG_FILE, OCFile.class));
            // TODO better in super, but needs to check ALL the class extending FileFragment; not right now

        mIgnoreFirstSavedState = args.getBoolean(ARG_IGNORE_FIRST);
        user = BundleExtensionsKt.getParcelableArgument(args, ARG_USER, User.class);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (getArguments() != null) {
            if (!mIgnoreFirstSavedState) {
                setFile(BundleExtensionsKt.getParcelableArgument(requireArguments(), EXTRA_FILE, OCFile.class));
                user = BundleExtensionsKt.getParcelableArgument(requireArguments(), EXTRA_USER, User.class);
                mError = requireArguments().getBoolean(EXTRA_ERROR);
                FileDownloadHelper.Companion.instance().downloadFile(user, getFile());
            }
            else {
                mIgnoreFirstSavedState = false;
            }
        }

        mView = inflater.inflate(R.layout.file_download_fragment, container, false);

        ProgressBar progressBar = mView.findViewById(R.id.progressBar);
        viewThemeUtils.platform.themeHorizontalProgressBar(progressBar);
        mProgressListener = new ProgressListener(progressBar);

        (mView.findViewById(R.id.cancelBtn)).setOnClickListener(this);

        (mView.findViewById(R.id.fileDownloadLL)).setOnClickListener(v -> {
            if (getActivity() instanceof PreviewImageActivity previewImageActivity) {
                previewImageActivity.toggleFullScreen();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(getFile(), TAG);
        outState.putParcelable(FileDownloadFragment.EXTRA_FILE, getFile());
        outState.putParcelable(FileDownloadFragment.EXTRA_USER, user);
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
        if (v.getId() == R.id.cancelBtn) {
            containerActivity.getFileOperationsHelper().cancelTransference(getFile());
            requireActivity().finish();
        } else {
            Log_OC.e(TAG, "Incorrect view clicked!");
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
        if (mProgressListener != null && !mListening && containerActivity.getFileDownloadProgressListener() != null) {
            containerActivity.getFileDownloadProgressListener().addDataTransferProgressListener(mProgressListener, getFile());
            mListening = true;
            setButtonsForTransferring();
        }
    }


    public void leaveTransferProgress() {
        if (mProgressListener != null && containerActivity.getFileDownloadProgressListener() != null) {
            containerActivity.getFileDownloadProgressListener()
                .removeDataTransferProgressListener(mProgressListener, getFile());
            mListening = false;
        }
    }


    /**
     * Helper class responsible for updating the progress bar shown for file uploading or downloading
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent;
        WeakReference<ProgressBar> mProgressBar;

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
