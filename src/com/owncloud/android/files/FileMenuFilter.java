/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.files;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest. 
 */
public class FileMenuFilter {

    private OCFile mFile;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;

    /**
     * Constructor
     *
     * @param targetFile        {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access the
     *                          {@link FileUploader} and {@link FileDownloader} services
     * @param context           Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(OCFile targetFile, Account account, ComponentsGetter cg,
                          Context context) {
        mFile = targetFile;
        mAccount = account;
        mComponentsGetter = cg;
        mContext = context;
    }


    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     *
     * @param menu              Options or context menu to filter.
     */
    public void filter(Menu menu) {
        List<Integer> toShow = new ArrayList<Integer>();
        List<Integer> toHide = new ArrayList<Integer>();

        filter(toShow, toHide);

        MenuItem item = null;
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
            }
        }

        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }


    /**
     * Performs the real filtering, to be applied in the {@link Menu} by the caller methods.
     *
     * Decides what actions must be shown and hidden.
     *
     * @param toShow            List to save the options that must be shown in the menu.
     * @param toHide            List to save the options that must be shown in the menu.
     */
    private void filter(List<Integer> toShow, List <Integer> toHide) {
        boolean synchronizing = false;
        if (mComponentsGetter != null && mFile != null && mAccount != null) {
            OperationsServiceBinder opsBinder = mComponentsGetter.getOperationsServiceBinder();
            FileUploaderBinder uploaderBinder = mComponentsGetter.getFileUploaderBinder();
            FileDownloaderBinder downloaderBinder = mComponentsGetter.getFileDownloaderBinder();
            synchronizing = (
                // comparing local and remote
                (opsBinder != null && opsBinder.isSynchronizing(mAccount, mFile.getRemotePath())) ||
                // downloading
                (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile)) ||
                // uploading
                (uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile))
            );
        }

        /// decision is taken for each possible action on a file in the menu

        // DOWNLOAD 
        if (mFile == null || mFile.isDown() || mFile.isFolder() || synchronizing) {
            toHide.add(R.id.action_download_file);

        } else {
            toShow.add(R.id.action_download_file);
        }

        // RENAME
        if (mFile == null || synchronizing) {
            toHide.add(R.id.action_rename_file);

        } else {
            toShow.add(R.id.action_rename_file);
        }

        // MOVE & COPY
        if (mFile == null || synchronizing) {
            toHide.add(R.id.action_move);
            toHide.add(R.id.action_copy);
        } else {
            toShow.add(R.id.action_move);
            toShow.add(R.id.action_copy);
        }

        // REMOVE
        if (mFile == null || synchronizing) {
            toHide.add(R.id.action_remove_file);

        } else {
            toShow.add(R.id.action_remove_file);
        }

        // OPEN WITH (different to preview!)
        if (mFile == null || mFile.isFolder() || !mFile.isDown() || synchronizing) {
            toHide.add(R.id.action_open_file_with);

        } else {
            toShow.add(R.id.action_open_file_with);
        }

        // CANCEL SYNCHRONIZATION
        if (mFile == null || !synchronizing) {
            toHide.add(R.id.action_cancel_sync);

        } else {
            toShow.add(R.id.action_cancel_sync);
        }

        // SYNC CONTENTS (BOTH FILE AND FOLDER)
        if (mFile == null || (!mFile.isFolder() && !mFile.isDown()) || synchronizing) {
            toHide.add(R.id.action_sync_file);

        } else {
            toShow.add(R.id.action_sync_file);
        }

        // SHARE FILE
        // TODO add check on SHARE available on server side?
        boolean shareAllowed = (mContext != null  &&
                mContext.getString(R.string.share_feature).equalsIgnoreCase("on"));
        if (!shareAllowed || mFile == null) {
            toHide.add(R.id.action_share_file);
        } else {
            toShow.add(R.id.action_share_file);
        }

        // UNSHARE FILE
        // TODO add check on SHARE available on server side?
        if ( !shareAllowed || (mFile == null || !mFile.isSharedViaLink())) {
            toHide.add(R.id.action_unshare_file);
        } else {
            toShow.add(R.id.action_unshare_file);
        }

        // SHARE FILE, with Users
        if (!shareAllowed ||  mFile == null) {
            toHide.add(R.id.action_share_with_users);
        } else {
            toShow.add(R.id.action_share_with_users);
        }


        // SEE DETAILS
        if (mFile == null || mFile.isFolder()) {
            toHide.add(R.id.action_see_details);
        } else {
            toShow.add(R.id.action_see_details);
        }

        // SEND
        boolean sendAllowed = (mContext != null &&
                mContext.getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on"));
        if (mFile == null || !sendAllowed || mFile.isFolder() || synchronizing) {
            toHide.add(R.id.action_send_file);
        } else {
            toShow.add(R.id.action_send_file);
        }

        // FAVORITES
        if (mFile == null || synchronizing || mFile.isFolder() || mFile.isFavorite()) {
            toHide.add(R.id.action_favorite_file);
        } else {
            toShow.add(R.id.action_favorite_file);
        }

        // UNFAVORITES
        if (mFile == null || synchronizing || mFile.isFolder() || !mFile.isFavorite()) {
            toHide.add(R.id.action_unfavorite_file);
        } else {
            toShow.add(R.id.action_unfavorite_file);
        }

    }

}
