/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Andy Scherzinger
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import com.owncloud.android.utils.MimeTypeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest. 
 */
public class FileMenuFilter {

    private static final int SINGLE_SELECT_ITEMS = 1;

    private int mNumberOfAllFiles;
    private Collection<OCFile> mFiles;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;
    private boolean mOverflowMenu;

    /**
     * Constructor
     *
     * @param numberOfAllFiles  Number of all displayed files
     * @param targetFiles       Collection of {@link OCFile} file targets of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     * @param overflowMenu      true if the overflow menu items are being filtered
     */
    public FileMenuFilter(int numberOfAllFiles, Collection<OCFile> targetFiles, Account account,
                          ComponentsGetter cg, Context context, boolean overflowMenu) {
        mNumberOfAllFiles = numberOfAllFiles;
        mFiles = targetFiles;
        mAccount = account;
        mComponentsGetter = cg;
        mContext = context;
        mOverflowMenu = overflowMenu;
    }

    /**
     * Constructor
     *
     * @param targetFile        {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     * @param overflowMenu      true if the overflow menu items are being filtered
     */
    public FileMenuFilter(OCFile targetFile, Account account, ComponentsGetter cg, Context context,
                          boolean overflowMenu) {
        this(1, Collections.singletonList(targetFile), account, cg, context, overflowMenu);
    }

    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     *
     * @param menu                  Options or context menu to filter.
     * @param inSingleFileFragment  True if this is not listing, but single file fragment, like preview or details.
     */
    public void filter(Menu menu, boolean inSingleFileFragment) {
        if (mFiles == null || mFiles.size() <= 0) {
            hideAll(menu);
        } else {
            List<Integer> toShow = new ArrayList<>();
            List<Integer> toHide = new ArrayList<>();

            filter(toShow, toHide, inSingleFileFragment);

            for (int i : toShow) {
                showMenuItem(menu.findItem(i));
            }

            for (int i : toHide) {
                hideMenuItem(menu.findItem(i));
            }
        }
    }

    public static void hideAll(Menu menu) {
        if (menu != null) {
            for (int i = 0; i < menu.size(); i++) {
                hideMenuItem(menu.getItem(i));
            }
        }
    }

    private static void hideMenuItem(MenuItem item) {
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }

    private static void showMenuItem(MenuItem item) {
        if (item != null) {
            item.setVisible(true);
            item.setEnabled(true);
        }
    }

    public static void hideMenuItems(MenuItem... items) {
        if (items != null) {
            for (MenuItem item : items) {
                hideMenuItem(item);
            }
        }
    }

    /**
     * Performs the real filtering, to be applied in the {@link Menu} by the caller methods.
     *
     * Decides what actions must be shown and hidden.
     *
     * @param toShow                List to save the options that must be shown in the menu.
     * @param toHide                List to save the options that must be shown in the menu.
     * @param inSingleFileFragment  True if this is not listing, but single file fragment, like preview or details.
     */
    private void filter(List<Integer> toShow, List<Integer> toHide, boolean inSingleFileFragment) {
        boolean synchronizing = anyFileSynchronizing();
        OCCapability capability = mComponentsGetter.getStorageManager().getCapability(mAccount.name);
        boolean endToEndEncryptionEnabled = capability != null && capability.getEndToEndEncryption().isTrue();

        filterDownload(toShow, toHide, synchronizing);
        filterRename(toShow, toHide, synchronizing);
        filterMoveCopy(toShow, toHide, synchronizing);
        filterRemove(toShow, toHide, synchronizing);
        filterSelectAll(toShow, toHide, inSingleFileFragment);
        filterDeselectAll(toShow, toHide, inSingleFileFragment);
        filterOpenWith(toShow, toHide, synchronizing);
        filterCancelSync(toShow, toHide, synchronizing);
        filterSync(toShow, toHide, synchronizing);
        filterShareFile(toShow, toHide, capability);
        filterDetails(toShow, toHide);
        filterKeepAvailableOffline(toShow, toHide, synchronizing);
        filterDontKeepAvailableOffline(toShow, toHide, synchronizing);
        filterFavorite(toShow, toHide, synchronizing);
        filterUnfavorite(toShow, toHide, synchronizing);
        filterEncrypt(toShow, toHide, endToEndEncryptionEnabled);
        filterUnsetEncrypted(toShow, toHide, endToEndEncryptionEnabled);
        filterSetPictureAs(toShow, toHide);
    }

    private void filterShareFile(List<Integer> toShow, List<Integer> toHide, OCCapability capability) {
        if (containsEncryptedFile() || (!isShareViaLinkAllowed() && !isShareWithUsersAllowed()) ||
                !isSingleSelection() || !isShareApiEnabled(capability) || mOverflowMenu) {
            toHide.add(R.id.action_send_share_file);
        } else {
            toShow.add(R.id.action_send_share_file);
        }
    }

    private void filterDetails(List<Integer> toShow, List<Integer> toHide) {
        if (isSingleSelection()) {
            toShow.add(R.id.action_see_details);
        } else {
            toHide.add(R.id.action_see_details);
        }
    }

    private void filterKeepAvailableOffline(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (!allFiles() || synchronizing || allKeptAvailableOffline()) {
            toHide.add(R.id.action_keep_files_offline);
        } else {
            toShow.add(R.id.action_keep_files_offline);
        }
    }

    private void filterDontKeepAvailableOffline(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (!allFiles() || synchronizing || allNotKeptAvailableOffline()) {
            toHide.add(R.id.action_unset_keep_files_offline);
        } else {
            toShow.add(R.id.action_unset_keep_files_offline);
        }
    }

    private void filterFavorite(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || synchronizing || allFavorites()) {
            toHide.add(R.id.action_favorite);
        } else {
            toShow.add(R.id.action_favorite);
        }
    }

    private void filterUnfavorite(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || synchronizing || allNotFavorites()) {
            toHide.add(R.id.action_unset_favorite);
        } else {
            toShow.add(R.id.action_unset_favorite);
        }
    }

    private void filterEncrypt(List<Integer> toShow, List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (mFiles.isEmpty() || !isSingleSelection() || isSingleFile() || isEncryptedFolder()
                || !endToEndEncryptionEnabled) {
            toHide.add(R.id.action_encrypted);
        } else {
            toShow.add(R.id.action_encrypted);
        }
    }

    private void filterUnsetEncrypted(List<Integer> toShow, List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (mFiles.isEmpty() || !isSingleSelection() || isSingleFile() || !isEncryptedFolder()
                || !endToEndEncryptionEnabled) {
            toHide.add(R.id.action_unset_encrypted);
        } else {
            toShow.add(R.id.action_unset_encrypted);
        }
    }

    private void filterSetPictureAs(List<Integer> toShow, List<Integer> toHide) {
        if (isSingleImage() && !MimeTypeUtil.isSVG(mFiles.iterator().next())) {
            toShow.add(R.id.action_set_as_wallpaper);
        } else {
            toHide.add(R.id.action_set_as_wallpaper);
        }
    }

    private void filterSync(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || (!anyFileDown() && !containsFolder()) || synchronizing) {
            toHide.add(R.id.action_sync_file);
        } else {
            toShow.add(R.id.action_sync_file);
        }
    }

    private void filterCancelSync(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || !synchronizing) {
            toHide.add(R.id.action_cancel_sync);
        } else {
            toShow.add(R.id.action_cancel_sync);
        }
    }

    private void filterOpenWith(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (!isSingleFile() || !anyFileDown() || synchronizing) {
            toHide.add(R.id.action_open_file_with);
        } else {
            toShow.add(R.id.action_open_file_with);
        }
    }

    private void filterDeselectAll(List<Integer> toShow, List<Integer> toHide, boolean inSingleFileFragment) {
        if (inSingleFileFragment) {
            // Always hide in single file fragments
            toHide.add(R.id.action_deselect_all_action_menu);
        } else {
            // Show only if at least one item is selected.
            if (mFiles.isEmpty() || mOverflowMenu) {
                toHide.add(R.id.action_deselect_all_action_menu);
            } else {
                toShow.add(R.id.action_deselect_all_action_menu);
            }
        }
    }

    private void filterSelectAll(List<Integer> toShow, List<Integer> toHide, boolean inSingleFileFragment) {
        if (!inSingleFileFragment) {
            // Show only if at least one item isn't selected.
            if (mFiles.size() >= mNumberOfAllFiles || mOverflowMenu) {
                toHide.add(R.id.action_select_all_action_menu);
            } else {
                toShow.add(R.id.action_select_all_action_menu);
            }
        } else {
            // Always hide in single file fragments
            toHide.add(R.id.action_select_all_action_menu);
        }
    }

    private void filterRemove(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || synchronizing || containsEncryptedFolder()) {
            toHide.add(R.id.action_remove_file);
        } else {
            toShow.add(R.id.action_remove_file);
        }
    }

    private void filterMoveCopy(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || synchronizing || containsEncryptedFile() || containsEncryptedFolder()) {
            toHide.add(R.id.action_move);
            toHide.add(R.id.action_copy);
        } else {
            toShow.add(R.id.action_move);
            toShow.add(R.id.action_copy);
        }
    }

    private void filterRename(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (!isSingleSelection() || synchronizing || containsEncryptedFile() || containsEncryptedFolder()) {
            toHide.add(R.id.action_rename_file);
        } else {
            toShow.add(R.id.action_rename_file);
        }
    }

    private void filterDownload(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (mFiles.isEmpty() || containsFolder() || anyFileDown() || synchronizing) {
            toHide.add(R.id.action_download_file);
        } else {
            toShow.add(R.id.action_download_file);
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
            for (Iterator<OCFile> iterator = mFiles.iterator(); !synchronizing && iterator.hasNext(); ) {
                synchronizing = opsBinder.isSynchronizing(mAccount, iterator.next());
            }
        }
        return synchronizing;
    }

    private boolean anyFileDownloading(FileDownloaderBinder downloaderBinder) {
        boolean downloading = false;
        if (downloaderBinder != null) {
            for (Iterator<OCFile> iterator = mFiles.iterator(); !downloading && iterator.hasNext(); ) {
                downloading = downloaderBinder.isDownloading(mAccount, iterator.next());
            }
        }
        return downloading;
    }

    private boolean anyFileUploading(FileUploaderBinder uploaderBinder) {
        boolean uploading = false;
        if (uploaderBinder != null) {
            for (Iterator<OCFile> iterator = mFiles.iterator(); !uploading && iterator.hasNext(); ) {
                uploading = uploaderBinder.isUploading(mAccount, iterator.next());
            }
        }
        return uploading;
    }

    private boolean isShareApiEnabled(OCCapability capability) {
        return capability != null &&
                (capability.getFilesSharingApiEnabled().isTrue() ||
                        capability.getFilesSharingApiEnabled().isUnknown()
                );
    }

    private boolean isShareWithUsersAllowed() {
        return mContext != null &&
                mContext.getResources().getBoolean(R.bool.share_with_users_feature);
    }

    private boolean isShareViaLinkAllowed() {
        return mContext != null &&
                mContext.getResources().getBoolean(R.bool.share_via_link_feature);
    }

    private boolean isSingleSelection() {
        return mFiles.size() == SINGLE_SELECT_ITEMS;
    }

    private boolean isSingleFile() {
        return isSingleSelection() && !mFiles.iterator().next().isFolder();
    }

    private boolean isEncryptedFolder() {
        if (isSingleSelection()) {
            OCFile file = mFiles.iterator().next();

            return file.isFolder() && file.isEncrypted();
        } else {
            return false;
        }
    }

    private boolean isSingleImage() {
        return isSingleSelection() && MimeTypeUtil.isImage(mFiles.iterator().next());
    }

    private boolean allFiles() {
        return mFiles != null && !containsFolder();
    }

    private boolean containsEncryptedFile() {
        for (OCFile file : mFiles) {
            if (!file.isFolder() && file.isEncrypted()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsEncryptedFolder() {
        for (OCFile file : mFiles) {
            if (file.isFolder() && file.isEncrypted()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFolder() {
        for (OCFile file : mFiles) {
            if (file.isFolder()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFileDown() {
        for (OCFile file : mFiles) {
            if (file.isDown()) {
                return true;
            }
        }
        return false;
    }

    private boolean allKeptAvailableOffline() {
        for (OCFile file : mFiles) {
            if (!file.isAvailableOffline()) {
                return false;
            }
        }
        return true;
    }

    private boolean allFavorites() {
        for (OCFile file : mFiles) {
            if (!file.getIsFavorite()) {
                return false;
            }
        }
        return true;
    }

    private boolean allNotFavorites() {
        for (OCFile file : mFiles) {
            if (file.getIsFavorite()) {
                return false;
            }
        }
        return true;
    }

    private boolean allNotKeptAvailableOffline() {
        for (OCFile file : mFiles) {
            if (file.isAvailableOffline()) {
                return false;
            }
        }
        return true;
    }
}
