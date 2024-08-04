/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey Vilas <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2018 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014-2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.files;

import android.accounts.AccountManager;
import android.content.Context;
import android.view.Menu;

import com.nextcloud.android.files.FileLockingHelper;
import com.nextcloud.client.account.User;
import com.nextcloud.client.editimage.EditImageActivity;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.utils.EditorUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.NextcloudServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.IdRes;
import androidx.core.content.pm.ShortcutManagerCompat;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest.
 */
public class FileMenuFilter {

    private static final int SINGLE_SELECT_ITEMS = 1;
    private static final int EMPTY_FILE_LENGTH = 0;
    public static final String SEND_OFF = "off";

    private final int numberOfAllFiles;
    private final Collection<OCFile> files;
    private final ComponentsGetter componentsGetter;
    private final Context context;
    private final boolean overflowMenu;
    private final User user;
    private final String userId;
    private final FileDataStorageManager storageManager;
    private final EditorUtils editorUtils;


    public static class Factory {
        private final FileDataStorageManager storageManager;
        private final Context context;
        private final EditorUtils editorUtils;

        @Inject
        public Factory(final FileDataStorageManager storageManager, final Context context, final EditorUtils editorUtils) {
            this.storageManager = storageManager;
            this.context = context;
            this.editorUtils = editorUtils;
        }

        /**
         * @param numberOfAllFiles Number of all displayed files
         * @param files            Collection of {@link OCFile} file targets of the action to filter in the {@link Menu}.
         * @param componentsGetter Accessor to app components, needed to access synchronization services
         * @param overflowMenu     true if the overflow menu items are being filtered
         * @param user             currently active user
         */
        public FileMenuFilter newInstance(final int numberOfAllFiles, final Collection<OCFile> files, final ComponentsGetter componentsGetter, boolean overflowMenu, User user) {
            return new FileMenuFilter(storageManager, editorUtils, numberOfAllFiles, files, componentsGetter, context, overflowMenu, user);
        }

        /**
         * @param file             {@link OCFile} file target
         * @param componentsGetter Accessor to app components, needed to access synchronization services
         * @param overflowMenu     true if the overflow menu items are being filtered
         * @param user             currently active user
         */
        public FileMenuFilter newInstance(final OCFile file, final ComponentsGetter componentsGetter, boolean overflowMenu, User user) {
            return newInstance(1, Collections.singletonList(file), componentsGetter, overflowMenu, user);
        }
    }


    private FileMenuFilter(FileDataStorageManager storageManager, EditorUtils editorUtils, int numberOfAllFiles,
                           Collection<OCFile> files,
                           ComponentsGetter componentsGetter,
                           Context context,
                           boolean overflowMenu,
                           User user
                          ) {
        this.storageManager = storageManager;
        this.editorUtils = editorUtils;
        this.numberOfAllFiles = numberOfAllFiles;
        this.files = files;
        this.componentsGetter = componentsGetter;
        this.context = context;
        this.overflowMenu = overflowMenu;
        this.user = user;
        userId = AccountManager
            .get(context)
            .getUserData(this.user.toPlatformAccount(),
                         com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);
    }

    /**
     * List of actions to remove given the parameters supplied in the constructor
     */
    @IdRes
    public List<Integer> getToHide(final boolean inSingleFileFragment){
        if(files != null && ! files.isEmpty()){
            return filter(inSingleFileFragment);
        }
        return null;
    }


    /**
     * Decides what actions must be shown and hidden implementing the different rule sets.
     *
     * @param inSingleFileFragment True if this is not listing, but single file fragment, like preview or details.
     */
    private List<Integer> filter(boolean inSingleFileFragment) {
        boolean synchronizing = anyFileSynchronizing();
        OCCapability capability = storageManager.getCapability(user.getAccountName());
        boolean endToEndEncryptionEnabled = capability.getEndToEndEncryption().isTrue();
        boolean fileLockingEnabled = capability.getFilesLockingVersion() != null;

        @IdRes final List<Integer> toHide = new ArrayList<>();

        filterEdit(toHide, capability);
        filterDownload(toHide, synchronizing);
        filterExport(toHide);
        filterRename(toHide, synchronizing);
        filterMoveOrCopy(toHide, synchronizing);
        filterRemove(toHide, synchronizing);
        filterSelectAll(toHide, inSingleFileFragment);
        filterDeselectAll(toHide, inSingleFileFragment);
        filterOpenWith(toHide, synchronizing);
        filterCancelSync(toHide, synchronizing);
        filterSync(toHide, synchronizing);
        filterShareFile(toHide, capability);
        filterSendFiles(toHide, inSingleFileFragment);
        filterDetails(toHide);
        filterFavorite(toHide, synchronizing);
        filterUnfavorite(toHide, synchronizing);
        filterEncrypt(toHide, endToEndEncryptionEnabled);
        filterUnsetEncrypted(toHide, endToEndEncryptionEnabled);
        filterSetPictureAs(toHide);
        filterStream(toHide);
        filterLock(toHide, fileLockingEnabled);
        filterUnlock(toHide, fileLockingEnabled);
        filterPinToHome(toHide);

        return toHide;
    }


    private void filterShareFile(List<Integer> toHide, OCCapability capability) {
        if (!isSingleSelection() || containsEncryptedFile() || hasEncryptedParent() ||
            (!isShareViaLinkAllowed() && !isShareWithUsersAllowed()) ||
            !isShareApiEnabled(capability) || !files.iterator().next().canReshare()) {
            toHide.add(R.id.action_send_share_file);
        }
    }

    private void filterSendFiles(List<Integer> toHide, boolean inSingleFileFragment) {
        if ((overflowMenu || SEND_OFF.equalsIgnoreCase(context.getString(R.string.send_files_to_other_apps)) || containsEncryptedFile()) ||
            (!inSingleFileFragment && (isSingleSelection() || !allFileDown())) ||
            !toHide.contains(R.id.action_send_share_file)) {
            toHide.add(R.id.action_send_file);
        }
    }

    private void filterDetails(Collection<Integer> toHide) {
        if (!isSingleSelection()) {
            toHide.add(R.id.action_see_details);
        }
    }

    private void filterFavorite(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || allFavorites()) {
            toHide.add(R.id.action_favorite);
        }
    }

    private void filterUnfavorite(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || allNotFavorites()) {
            toHide.add(R.id.action_unset_favorite);
        }
    }

    private void filterLock(List<Integer> toHide, boolean fileLockingEnabled) {
        if (files.isEmpty() ||
            !isSingleSelection() ||
            !fileLockingEnabled ||
            containsEncryptedFile() ||
            containsEncryptedFolder()) {
            toHide.add(R.id.action_lock_file);
        } else {
            OCFile file = files.iterator().next();
            if (file.isLocked() || file.isFolder()) {
                toHide.add(R.id.action_lock_file);
            }
        }
    }

    private void filterUnlock(List<Integer> toHide, boolean fileLockingEnabled) {
        if (files.isEmpty() || !isSingleSelection() || !fileLockingEnabled) {
            toHide.add(R.id.action_unlock_file);
        } else {
            OCFile file = files.iterator().next();
            if (!FileLockingHelper.canUserUnlockFile(userId, file)) {
                toHide.add(R.id.action_unlock_file);
            }
        }
    }

    private void filterEncrypt(List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (files.isEmpty() || !isSingleSelection() || isSingleFile() || isEncryptedFolder() || isGroupFolder()
            || !endToEndEncryptionEnabled || !isEmptyFolder() || isShared()) {
            toHide.add(R.id.action_encrypted);
        }
    }

    private void filterUnsetEncrypted(List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (!endToEndEncryptionEnabled || files.isEmpty() || !isSingleSelection() || isSingleFile() || !isEncryptedFolder() || hasEncryptedParent()
            || !isEmptyFolder() || !FileOperationsHelper.isEndToEndEncryptionSetup(context, user)) {
            toHide.add(R.id.action_unset_encrypted);
        }
    }

    private void filterSetPictureAs(List<Integer> toHide) {
        if (!isSingleImage() || MimeTypeUtil.isSVG(files.iterator().next())) {
            toHide.add(R.id.action_set_as_wallpaper);
        }
    }

    private void filterPinToHome(List<Integer> toHide) {
        if (!isSingleSelection() || !ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            toHide.add(R.id.action_pin_to_homescreen);
        }
    }

    private void filterEdit(
        List<Integer> toHide,
        OCCapability capability
                           ) {
        if (files.iterator().next().isEncrypted()) {
            toHide.add(R.id.action_edit);
            return;
        }

        String mimeType = files.iterator().next().getMimeType();

        if (!isRichDocumentEditingSupported(capability, mimeType) && !editorUtils.isEditorAvailable(user, mimeType) &&
            !(isSingleImage() && EditImageActivity.Companion.canBePreviewed(files.iterator().next()))) {
            toHide.add(R.id.action_edit);
        }
    }

    /**
     * This will be replaced by unified editor and can be removed once EOL of corresponding server version.
     */
    @NextcloudServer(max = 18)
    private boolean isRichDocumentEditingSupported(OCCapability capability, String mimeType) {
        return isSingleFile() &&
            (capability.getRichDocumentsMimeTypeList().contains(mimeType) ||
                capability.getRichDocumentsOptionalMimeTypeList().contains(mimeType)) &&
            capability.getRichDocumentsDirectEditing().isTrue();
    }

    private void filterSync(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || (!anyFileDown() && !containsFolder()) || synchronizing || containsEncryptedFile()
            || containsEncryptedFolder()) {
            toHide.add(R.id.action_sync_file);
        }
    }

    private void filterCancelSync(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || !synchronizing) {
            toHide.add(R.id.action_cancel_sync);
        }
    }

    private void filterOpenWith(Collection<Integer> toHide, boolean synchronizing) {
        if (!isSingleFile() || !anyFileDown() || synchronizing) {
            toHide.add(R.id.action_open_file_with);
        }
    }

    private void filterDeselectAll(List<Integer> toHide, boolean inSingleFileFragment) {
        if (inSingleFileFragment) {
            // Always hide in single file fragments
            toHide.add(R.id.action_deselect_all_action_menu);
        } else {
            // Show only if at least one item is selected.
            if (files.isEmpty() || overflowMenu) {
                toHide.add(R.id.action_deselect_all_action_menu);
            }
        }
    }

    private void filterSelectAll(List<Integer> toHide, boolean inSingleFileFragment) {
        if (!inSingleFileFragment) {
            // Show only if at least one item isn't selected.
            if (files.size() >= numberOfAllFiles || overflowMenu) {
                toHide.add(R.id.action_select_all_action_menu);
            }
        } else {
            // Always hide in single file fragments
            toHide.add(R.id.action_select_all_action_menu);
        }
    }

    private void filterRemove(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || containsLockedFile()
            || containsEncryptedFolder() || isFolderAndContainsEncryptedFile()) {
            toHide.add(R.id.action_remove_file);
        }
    }

    private void filterMoveOrCopy(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || containsEncryptedFile() || containsEncryptedFolder() || containsLockedFile()) {
            toHide.add(R.id.action_move_or_copy);
        }
    }

    private void filterRename(Collection<Integer> toHide, boolean synchronizing) {
        if (!isSingleSelection() || synchronizing || containsEncryptedFile() || containsEncryptedFolder() || containsLockedFile()) {
            toHide.add(R.id.action_rename_file);
        }
    }

    private void filterDownload(List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || containsFolder() || anyFileDown() || synchronizing) {
            toHide.add(R.id.action_download_file);
        }
    }

    private void filterExport(List<Integer> toHide) {
        if (files.isEmpty() || containsFolder()) {
            toHide.add(R.id.action_export_file);
        }
    }

    private void filterStream(List<Integer> toHide) {
        if (files.isEmpty() || !isSingleFile() || !isSingleMedia() || containsEncryptedFile()) {
            toHide.add(R.id.action_stream_media);
        }
    }

    private boolean anyFileSynchronizing() {
        boolean synchronizing = false;
        if (componentsGetter != null && !files.isEmpty() && user != null) {
            OperationsServiceBinder opsBinder = componentsGetter.getOperationsServiceBinder();
            synchronizing = anyFileSynchronizing(opsBinder) ||      // comparing local and remote
                anyFileDownloading() ||
                anyFileUploading();
        }
        return synchronizing;
    }

    private boolean anyFileSynchronizing(OperationsServiceBinder opsBinder) {
        boolean synchronizing = false;
        if (opsBinder != null) {
            for (Iterator<OCFile> iterator = files.iterator(); !synchronizing && iterator.hasNext(); ) {
                synchronizing = opsBinder.isSynchronizing(user, iterator.next());
            }
        }
        return synchronizing;
    }

    private boolean anyFileDownloading() {
        for (OCFile file : files) {
            if (FileDownloadHelper.Companion.instance().isDownloading(user, file)) {
                return true;
            }
        }

        return false;
    }

    private boolean anyFileUploading() {
        for (OCFile file : files) {
            if (FileUploadHelper.Companion.instance().isUploading(user, file)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShareApiEnabled(OCCapability capability) {
        return capability != null &&
            (capability.getFilesSharingApiEnabled().isTrue() ||
                capability.getFilesSharingApiEnabled().isUnknown()
            );
    }

    private boolean isShareWithUsersAllowed() {
        return context != null &&
            context.getResources().getBoolean(R.bool.share_with_users_feature);
    }

    private boolean isShareViaLinkAllowed() {
        return context != null &&
            context.getResources().getBoolean(R.bool.share_via_link_feature);
    }

    private boolean isSingleSelection() {
        return files.size() == SINGLE_SELECT_ITEMS;
    }

    private boolean isSingleFile() {
        return isSingleSelection() && !files.iterator().next().isFolder();
    }

    private boolean isEncryptedFolder() {
        if (isSingleSelection()) {
            OCFile file = files.iterator().next();

            return file.isFolder() && file.isEncrypted();
        } else {
            return false;
        }
    }

    private boolean isEmptyFolder() {
        if (isSingleSelection()) {
            OCFile file = files.iterator().next();

            boolean noChildren = storageManager
                .getFolderContent(file, false).size() == EMPTY_FILE_LENGTH;

            return file.isFolder() && file.getFileLength() == EMPTY_FILE_LENGTH && noChildren;
        } else {
            return false;
        }
    }

    private boolean isGroupFolder() {
        return files.iterator().next().isGroupFolder();
    }

    private boolean hasEncryptedParent() {
        OCFile folder = files.iterator().next();
        OCFile parent = storageManager.getFileById(folder.getParentId());

        return parent != null && parent.isEncrypted();
    }

    private boolean isSingleImage() {
        return isSingleSelection() && MimeTypeUtil.isImage(files.iterator().next());
    }

    private boolean isSingleMedia() {
        OCFile file = files.iterator().next();
        return isSingleSelection() && (MimeTypeUtil.isVideo(file) || MimeTypeUtil.isAudio(file));
    }

    private boolean isFolderAndContainsEncryptedFile() {
        for (OCFile file : files) {
            if (!file.isFolder()) {
                continue;
            }
            if (file.isFolder()) {
                List<OCFile> children = storageManager.getFolderContent(file, false);
                for (OCFile child : children) {
                    if (child.isEncrypted()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean containsEncryptedFile() {
        for (OCFile file : files) {
            if (!file.isFolder() && file.isEncrypted()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLockedFile() {
        for (OCFile file : files) {
            if (file.isLocked()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsEncryptedFolder() {
        for (OCFile file : files) {
            if (file.isFolder() && file.isEncrypted()) {
                return true;
            }
        }
        return false;
    }

    private boolean containsFolder() {
        for (OCFile file : files) {
            if (file.isFolder()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFileDown() {
        for (OCFile file : files) {
            if (file.isDown()) {
                return true;
            }
        }
        return false;
    }

    private boolean allFileDown() {
        for (OCFile file: files) {
            if(!file.isDown()) {
                return false;
            }
        }
        return true;
    }

    private boolean allFavorites() {
        for (OCFile file : files) {
            if (!file.isFavorite()) {
                return false;
            }
        }
        return true;
    }

    private boolean allNotFavorites() {
        for (OCFile file : files) {
            if (file.isFavorite()) {
                return false;
            }
        }
        return true;
    }

    private boolean isShared() {
        for (OCFile file : files) {
            if (file.isSharedWithMe() || file.isSharedViaLink() || file.isSharedWithSharee()) {
                return true;
            }
        }
        return false;
    }
}
