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

import android.accounts.Account;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest. 
 */
public class FileMenuFilter {

    private static final int SINGLE_SELECT_ITEMS = 1;

    private Collection<OCFile> mFiles;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;

    /**
     * Constructor
     *
     * @param targetFiles       Collection of {@link OCFile} file targets of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(Collection<OCFile> targetFiles, Account account, ComponentsGetter cg,
                          Context context) {
        mFiles = targetFiles;
        mAccount = account;
        mComponentsGetter = cg;
        mContext = context;
    }

    /**
     * Constructor
     *
     * @param targetFile        {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(OCFile targetFile, Account account, ComponentsGetter cg,
                          Context context) {
        this(Arrays.asList(new OCFile[]{targetFile}), account, cg, context);
    }

    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     *
     * @param menu              Options or context menu to filter.
     */
    public void filter(Menu menu) {
        if (mFiles == null || mFiles.size() <= 0) {
            hideAll(menu);

        } else {
            List<Integer> toShow = new ArrayList<Integer>();
            List<Integer> toHide = new ArrayList<Integer>();

            filter(toShow, toHide);

            MenuItem item;
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
    }

    private void hideAll(Menu menu) {
        MenuItem item;
        for (int i=0; i<menu.size(); i++) {
            item = menu.getItem(i);
            item.setVisible(false);
            item.setEnabled(false);
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
        boolean synchronizing = anyFileSynchronizing();

        /// decision is taken for each possible action on a file in the menu

        // DOWNLOAD 
        if (mFiles.isEmpty() || containsFolder() || anyFileDown() || synchronizing) {
            toHide.add(R.id.action_download_file);

        } else {
            toShow.add(R.id.action_download_file);
        }

        // RENAME
        if (!isSingleSelection() || synchronizing) {
            toHide.add(R.id.action_rename_file);

        } else {
            toShow.add(R.id.action_rename_file);
        }

        // MOVE & COPY
        if (mFiles.isEmpty() || synchronizing) {
            toHide.add(R.id.action_move);
            toHide.add(R.id.action_copy);
        } else {
            toShow.add(R.id.action_move);
            toShow.add(R.id.action_copy);
        }

        // REMOVE
        if (mFiles.isEmpty() || synchronizing) {
            toHide.add(R.id.action_remove_file);

        } else {
            toShow.add(R.id.action_remove_file);
        }

        // OPEN WITH (different to preview!)
        if (!isSingleFile() || !anyFileDown() || synchronizing) {
            toHide.add(R.id.action_open_file_with);

        } else {
            toShow.add(R.id.action_open_file_with);
        }

        // CANCEL SYNCHRONIZATION
        if (mFiles.isEmpty() || !synchronizing) {
            toHide.add(R.id.action_cancel_sync);

        } else {
            toShow.add(R.id.action_cancel_sync);
        }

        // SYNC CONTENTS (BOTH FILE AND FOLDER)
        if (mFiles.isEmpty() || (!anyFileDown() && !containsFolder()) || synchronizing) {
            toHide.add(R.id.action_sync_file);

        } else {
            toShow.add(R.id.action_sync_file);
        }

        // SHARE FILE
        boolean shareViaLinkAllowed = (mContext != null  &&
                mContext.getResources().getBoolean(R.bool.share_via_link_feature));
        boolean shareWithUsersAllowed = (mContext != null  &&
                mContext.getResources().getBoolean(R.bool.share_with_users_feature));

        OCCapability capability = mComponentsGetter.getStorageManager().getCapability(mAccount.name);
        boolean shareApiEnabled  = capability != null &&
                (capability.getFilesSharingApiEnabled().isTrue() ||
                        capability.getFilesSharingApiEnabled().isUnknown()
                );
        if ((!shareViaLinkAllowed && !shareWithUsersAllowed) || !isSingleSelection() || !shareApiEnabled) {
            toHide.add(R.id.action_share_file);
        } else {
            toShow.add(R.id.action_share_file);
        }

        // SEE DETAILS
        if (!isSingleFile()) {
            toHide.add(R.id.action_see_details);
        } else {
            toShow.add(R.id.action_see_details);
        }

        // SEND
        boolean sendAllowed = (mContext != null &&
                mContext.getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on"));
        if (!isSingleFile() || !sendAllowed || synchronizing) {
            toHide.add(R.id.action_send_file);
        } else {
            toShow.add(R.id.action_send_file);
        }

        // FAVORITES
        if (!allFiles() || synchronizing || allFavorites()) {
            toHide.add(R.id.action_favorite_file);
        } else {
            toShow.add(R.id.action_favorite_file);
        }

        // UNFAVORITES
        if (!allFiles() || synchronizing || allUnfavorites()) {
            toHide.add(R.id.action_unfavorite_file);
        } else {
            toShow.add(R.id.action_unfavorite_file);
        }

        // SET PICTURE AS
        if (mFile == null || (!mFile.isAudio() && !mFile.isVideo())){
            toHide.add(R.id.action_set_as_wallpaper);
        } else {
            toShow.add(R.id.action_set_as_wallpaper);
        }

    }

    private boolean anyFileSynchronizing() {
        boolean synchronizing = false;
        if (mComponentsGetter != null && !mFiles.isEmpty() && mAccount != null) {
            OperationsServiceBinder opsBinder = mComponentsGetter.getOperationsServiceBinder();
            FileUploaderBinder uploaderBinder = mComponentsGetter.getFileUploaderBinder();
            FileDownloaderBinder downloaderBinder = mComponentsGetter.getFileDownloaderBinder();
            synchronizing = (
                anyFileSynchronizing(opsBinder) ||      // comparing local and remote
                anyFileDownloading(downloaderBinder) ||
                anyFileUploading(uploaderBinder)
            );
        }
        return synchronizing;
    }

    private boolean anyFileSynchronizing(OperationsServiceBinder opsBinder) {
        boolean synchronizing = false;
        if (opsBinder != null) {
            for (Iterator<OCFile> iterator = mFiles.iterator(); !synchronizing && iterator.hasNext();) {
                synchronizing = opsBinder.isSynchronizing(mAccount, iterator.next());
            }
        }
        return synchronizing;
    }

    private boolean anyFileDownloading(FileDownloaderBinder downloaderBinder) {
        boolean downloading = false;
        if (downloaderBinder != null) {
            for (Iterator<OCFile> iterator = mFiles.iterator(); !downloading && iterator.hasNext();) {
                downloading = downloaderBinder.isDownloading(mAccount, iterator.next());
            }
        }
        return downloading;
    }

    private boolean anyFileUploading(FileUploaderBinder uploaderBinder) {
        boolean uploading = false;
        if (uploaderBinder != null) {
            for (Iterator<OCFile> iterator = mFiles.iterator(); !uploading && iterator.hasNext();) {
                uploading = uploaderBinder.isUploading(mAccount, iterator.next());
            }
        }
        return uploading;
    }

    private boolean isSingleSelection() {
        return mFiles.size() == SINGLE_SELECT_ITEMS;
    }

    private boolean isSingleFile() {
        return isSingleSelection() && !mFiles.iterator().next().isFolder();
    }

    private boolean allFiles() {
        return mFiles != null && !containsFolder();
    }

    private boolean containsFolder() {
        for(OCFile file: mFiles) {
            if(file.isFolder()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFileDown() {
        for(OCFile file: mFiles) {
            if(file.isDown()) {
                return true;
            }
        }
        return false;
    }

    private boolean allFavorites() {
        for(OCFile file: mFiles) {
            if(!file.isFavorite()) {
                return false;
            }
        }
        return true;
    }

    private boolean allUnfavorites() {
        for(OCFile file: mFiles) {
            if(file.isFavorite()) {
                return false;
            }
        }
        return true;
    }
}
