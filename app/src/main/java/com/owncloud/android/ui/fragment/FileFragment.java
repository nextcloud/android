/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2013 David A. Velasco<dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment;

import android.app.Activity;
import android.os.Bundle;

import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.ComponentsGetter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;

/**
 * Common methods for {@link Fragment}s containing {@link OCFile}s
 */
public class FileFragment extends Fragment {

    private OCFile file;

    protected ContainerActivity containerActivity;

    /**
     * Creates an empty fragment.
     * It's necessary to keep a public constructor without parameters; the system uses it when
     * tries to reinstantiate a fragment automatically.
     */
    public FileFragment() {
        file = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            setFile(BundleExtensionsKt.getParcelableArgument(bundle, EXTRA_FILE, OCFile.class));
        }
    }

    /**
     * Creates an instance for a given {@OCFile}.
     *
     * @param file
     */
    public static FileFragment newInstance(OCFile file) {
        FileFragment fileFragment = new FileFragment();
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_FILE, file);
        fileFragment.setArguments(bundle);

        return fileFragment;
    }

    /**
     * Getter for the hold {@link OCFile}
     *
     * @return The {@link OCFile} hold
     */
    public OCFile getFile() {
        return file;
    }


    protected void setFile(OCFile file) {
        this.file = file;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            containerActivity = (ContainerActivity) activity;

        } catch (ClassCastException e) {
            throw new IllegalArgumentException(activity.toString() + " must implement " +
                                                   ContainerActivity.class.getSimpleName(), e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetach() {
        containerActivity = null;
        super.onDetach();
    }


    /**
     * Interface to implement by any Activity that includes some instance of FileListFragment
     * Interface to implement by any Activity that includes some instance of FileFragment
     */
    public interface ContainerActivity extends ComponentsGetter {

        /**
         * Request the parent activity to show the details of an {@link OCFile}.
         *
         * @param file      File to show details
         */
        void showDetails(OCFile file);

        /**
         * Request the parent activity to show the details of an {@link OCFile}.
         *
         * @param file      File to show details
         * @param activeTab the active tab
         */
        void showDetails(OCFile file, int activeTab);


        ///// TO UNIFY IN A SINGLE CALLBACK METHOD - EVENT NOTIFICATIONs  -> something happened
        // inside the fragment, MAYBE activity is interested --> unify in notification method
        /**
         * Callback method invoked when a the user browsed into a different folder through the
         * list of files
         *
         * @param folder
         */
        void onBrowsedDownTo(OCFile folder);

        /**
         * Callback method invoked when a the 'transfer state' of a file changes.
         *
         * This happens when a download or upload is started or ended for a file.
         *
         * This method is necessary by now to update the user interface of the double-pane layout
         * in tablets because methods FileDownloaderBinder.isDownloading(Account, OCFile)
         * and FilesUploadHelper.isUploading(Account, OCFile)
         * won't provide the needed response before the method where this is called finishes.
         *
         * TODO Remove this when the transfer state of a file is kept in the database
         * (other thing TODO)
         *
         * @param file          OCFile which state changed.
         * @param downloading   Flag signaling if the file is now downloading.
         * @param uploading     Flag signaling if the file is now uploading.
         */
        void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading);

        void showSortListGroup(boolean show);
    }

}
