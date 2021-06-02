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

import android.content.ContentResolver;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.DirectEditing;
import com.owncloud.android.lib.common.Editor;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.NextcloudServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile}
 * according to the current state of the latest.
 */
public class FileMenuFilter {

    private static final int SINGLE_SELECT_ITEMS = 1;
    public static final String SEND_OFF = "off";

    private int numberOfAllFiles;
    private Collection<OCFile> files;
    private ComponentsGetter componentsGetter;
    private Context context;
    private boolean overflowMenu;
    private User user;

    /**
     * Constructor
     *
     * @param numberOfAllFiles  Number of all displayed files
     * @param files             Collection of {@link OCFile} file targets of the action to filter in the {@link Menu}.
     * @param componentsGetter  Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     * @param overflowMenu      true if the overflow menu items are being filtered
     * @param user              currently active user
     */
    public FileMenuFilter(int numberOfAllFiles,
                          Collection<OCFile> files,
                          ComponentsGetter componentsGetter,
                          Context context,
                          boolean overflowMenu,
                          User user
    ) {
        this.numberOfAllFiles = numberOfAllFiles;
        this.files = files;
        this.componentsGetter = componentsGetter;
        this.context = context;
        this.overflowMenu = overflowMenu;
        this.user = user;
    }

    /**
     * Constructor
     *
     * @param file              {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param componentsGetter  Accessor to app components, needed to access synchronization services
     * @param context           Android {@link Context}, needed to access build setup resources.
     * @param overflowMenu      true if the overflow menu items are being filtered
     * @param user              currently active user
     */
    public FileMenuFilter(OCFile file,
                          ComponentsGetter componentsGetter,
                          Context context,
                          boolean overflowMenu,
                          User user
    ) {
        this(1, Collections.singletonList(file), componentsGetter, context, overflowMenu, user);
    }

    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account the state of the {@link
     * OCFile} held by the filter.
     *
     * @param menu                 Options or context menu to filter.
     * @param inSingleFileFragment True if this is not listing, but single file fragment, like preview or details.
     */
    public void filter(Menu menu, boolean inSingleFileFragment) {
        if (files == null || files.isEmpty()) {
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

    /**
     * hides a given {@link MenuItem}.
     *
     * @param item the {@link MenuItem} to be hidden
     */
    public static void hideMenuItem(MenuItem item) {
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
     * Decides what actions must be shown and hidden implementing the different rule sets.
     *  @param toShow                List to save the options that must be shown in the menu.
     * @param toHide                List to save the options that must be shown in the menu.
     * @param inSingleFileFragment  True if this is not listing, but single file fragment, like preview or details.
     */
    private void filter(List<Integer> toShow,
                        List<Integer> toHide,
                        boolean inSingleFileFragment) {
        boolean synchronizing = anyFileSynchronizing();
        OCCapability capability = componentsGetter.getStorageManager().getCapability(user.getAccountName());
        boolean endToEndEncryptionEnabled = capability.getEndToEndEncryption().isTrue();

        filterEdit(toShow, toHide, capability);
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
        filterSendFiles(toShow, toHide);
        filterDetails(toShow, toHide);
        filterFavorite(toShow, toHide, synchronizing);
        filterUnfavorite(toShow, toHide, synchronizing);
        filterEncrypt(toShow, toHide, endToEndEncryptionEnabled);
        filterUnsetEncrypted(toShow, toHide, endToEndEncryptionEnabled);
        filterSetPictureAs(toShow, toHide);
        filterStream(toShow, toHide);
    }

    private void filterShareFile(List<Integer> toShow, List<Integer> toHide, OCCapability capability) {
        if (containsEncryptedFile() || (!isShareViaLinkAllowed() && !isShareWithUsersAllowed()) ||
            !isSingleSelection() || !isShareApiEnabled(capability) || !files.iterator().next().canReshare()
            || overflowMenu) {
            toHide.add(R.id.action_send_share_file);
        } else {
            toShow.add(R.id.action_send_share_file);
        }
    }

    private void filterSendFiles(List<Integer> toShow, List<Integer> toHide) {
        if (containsEncryptedFile() || isSingleSelection() || overflowMenu || !anyFileDown() ||
            SEND_OFF.equalsIgnoreCase(context.getString(R.string.send_files_to_other_apps))) {
            toHide.add(R.id.action_send_file);
        } else {
            toShow.add(R.id.action_send_file);
        }
    }

    private void filterDetails(Collection<Integer> toShow, Collection<Integer> toHide) {
        if (isSingleSelection()) {
            toShow.add(R.id.action_see_details);
        } else {
            toHide.add(R.id.action_see_details);
        }
    }

    private void filterFavorite(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || allFavorites()) {
            toHide.add(R.id.action_favorite);
        } else {
            toShow.add(R.id.action_favorite);
        }
    }

    private void filterUnfavorite(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || allNotFavorites()) {
            toHide.add(R.id.action_unset_favorite);
        } else {
            toShow.add(R.id.action_unset_favorite);
        }
    }

    private void filterEncrypt(List<Integer> toShow, List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (files.isEmpty() || !isSingleSelection() || isSingleFile() || isEncryptedFolder()
                || !endToEndEncryptionEnabled) {
            toHide.add(R.id.action_encrypted);
        } else {
            toShow.add(R.id.action_encrypted);
        }
    }

    private void filterUnsetEncrypted(List<Integer> toShow, List<Integer> toHide, boolean endToEndEncryptionEnabled) {
        if (files.isEmpty() || !isSingleSelection() || isSingleFile() || !isEncryptedFolder()
                || !endToEndEncryptionEnabled) {
            toHide.add(R.id.action_unset_encrypted);
        } else {
            toShow.add(R.id.action_unset_encrypted);
        }
    }

    private void filterSetPictureAs(List<Integer> toShow, List<Integer> toHide) {
        if (isSingleImage() && !MimeTypeUtil.isSVG(files.iterator().next())) {
            toShow.add(R.id.action_set_as_wallpaper);
        } else {
            toHide.add(R.id.action_set_as_wallpaper);
        }
    }

    private void filterEdit(List<Integer> toShow,
                            List<Integer> toHide,
                            OCCapability capability
    ) {
        if (files.iterator().next().isEncrypted()) {
            toHide.add(R.id.action_edit);
            return;
        }

        String mimeType = files.iterator().next().getMimeType();

        if (isRichDocumentEditingSupported(capability, mimeType) || isEditorAvailable(context.getContentResolver(),
                                                                                      user,
                                                                                      mimeType)) {
            toShow.add(R.id.action_edit);
        } else {
            toHide.add(R.id.action_edit);
        }
    }

    public static boolean isEditorAvailable(ContentResolver contentResolver, User user, String mimeType) {
        return getEditor(contentResolver, user, mimeType) != null;
    }

    @Nullable
    public static Editor getEditor(ContentResolver contentResolver, User user, String mimeType) {
        String json = new ArbitraryDataProvider(contentResolver).getValue(user, ArbitraryDataProvider.DIRECT_EDITING);

        if (json.isEmpty()) {
            return null;
        }

        DirectEditing directEditing = new Gson().fromJson(json, DirectEditing.class);

        for (Editor editor : directEditing.editors.values()) {
            if (editor.mimetypes.contains(mimeType)) {
                return editor;
            }
        }

        for (Editor editor : directEditing.editors.values()) {
            if (editor.optionalMimetypes.contains(mimeType)) {
                return editor;
            }
        }

        return null;
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

    private void filterSync(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || (!anyFileDown() && !containsFolder()) || synchronizing) {
            toHide.add(R.id.action_sync_file);
        } else {
            toShow.add(R.id.action_sync_file);
        }
    }

    private void filterCancelSync(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || !synchronizing) {
            toHide.add(R.id.action_cancel_sync);
        } else {
            toShow.add(R.id.action_cancel_sync);
        }
    }

    private void filterOpenWith(Collection<Integer> toShow, Collection<Integer> toHide, boolean synchronizing) {
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
            if (files.isEmpty() || overflowMenu) {
                toHide.add(R.id.action_deselect_all_action_menu);
            } else {
                toShow.add(R.id.action_deselect_all_action_menu);
            }
        }
    }

    private void filterSelectAll(List<Integer> toShow, List<Integer> toHide, boolean inSingleFileFragment) {
        if (!inSingleFileFragment) {
            // Show only if at least one item isn't selected.
            if (files.size() >= numberOfAllFiles || overflowMenu) {
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
        if (files.isEmpty() || synchronizing || containsEncryptedFolder()) {
            toHide.add(R.id.action_remove_file);
        } else {
            toShow.add(R.id.action_remove_file);
        }
    }

    private void filterMoveCopy(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || synchronizing || containsEncryptedFile() || containsEncryptedFolder()) {
            toHide.add(R.id.action_move);
            toHide.add(R.id.action_copy);
        } else {
            toShow.add(R.id.action_move);
            toShow.add(R.id.action_copy);
        }
    }

    private void filterRename(Collection<Integer> toShow, Collection<Integer> toHide, boolean synchronizing) {
        if (!isSingleSelection() || synchronizing || containsEncryptedFile() || containsEncryptedFolder()) {
            toHide.add(R.id.action_rename_file);
        } else {
            toShow.add(R.id.action_rename_file);
        }
    }

    private void filterDownload(List<Integer> toShow, List<Integer> toHide, boolean synchronizing) {
        if (files.isEmpty() || containsFolder() || anyFileDown() || synchronizing) {
            toHide.add(R.id.action_download_file);
        } else {
            toShow.add(R.id.action_download_file);
        }
    }

    private void filterStream(List<Integer> toShow, List<Integer> toHide) {
        if (files.isEmpty() || !isSingleFile() || !isSingleMedia()) {
            toHide.add(R.id.action_stream_media);
        } else {
            toShow.add(R.id.action_stream_media);
        }
    }

    private boolean anyFileSynchronizing() {
        boolean synchronizing = false;
        if (componentsGetter != null && !files.isEmpty() && user != null) {
            OperationsServiceBinder opsBinder = componentsGetter.getOperationsServiceBinder();
            FileUploaderBinder uploaderBinder = componentsGetter.getFileUploaderBinder();
            FileDownloaderBinder downloaderBinder = componentsGetter.getFileDownloaderBinder();
            synchronizing = anyFileSynchronizing(opsBinder) ||      // comparing local and remote
                            anyFileDownloading(downloaderBinder) ||
                            anyFileUploading(uploaderBinder);
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

    private boolean anyFileDownloading(FileDownloaderBinder downloaderBinder) {
        boolean downloading = false;
        if (downloaderBinder != null) {
            for (Iterator<OCFile> iterator = files.iterator(); !downloading && iterator.hasNext(); ) {
                downloading = downloaderBinder.isDownloading(user, iterator.next());
            }
        }
        return downloading;
    }

    private boolean anyFileUploading(FileUploaderBinder uploaderBinder) {
        boolean uploading = false;
        if (uploaderBinder != null) {
            for (Iterator<OCFile> iterator = files.iterator(); !uploading && iterator.hasNext(); ) {
                uploading = uploaderBinder.isUploading(user, iterator.next());
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

    private boolean isSingleImage() {
        return isSingleSelection() && MimeTypeUtil.isImage(files.iterator().next());
    }

    private boolean isSingleMedia() {
        OCFile file = files.iterator().next();
        return isSingleSelection() && (MimeTypeUtil.isVideo(file) || MimeTypeUtil.isAudio(file));
    }

    private boolean containsEncryptedFile() {
        for (OCFile file : files) {
            if (!file.isFolder() && file.isEncrypted()) {
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
}
