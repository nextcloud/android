/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsSharingFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ShareeListAdapter;
import com.owncloud.android.ui.adapter.ShareeListAdapterListener;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.NoteDialogFragment;
import com.owncloud.android.ui.dialog.RenamePublicShareDialogFragment;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import static com.owncloud.android.lib.resources.shares.OCShare.CREATE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.DELETE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER;
import static com.owncloud.android.lib.resources.shares.OCShare.NO_PERMISSION;
import static com.owncloud.android.lib.resources.shares.OCShare.READ_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.SHARE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.UPDATE_PERMISSION_FLAG;

public class FileDetailSharingFragment extends Fragment implements ShareeListAdapterListener,
    DisplayUtils.AvatarGenerationListener,
    Injectable {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    public static final int PERMISSION_EDITING_ALLOWED = 17;

    private OCFile file;
    private User user;
    private OCCapability capabilities;

    private FileOperationsHelper fileOperationsHelper;
    private FileActivity fileActivity;
    private FileDataStorageManager fileDataStorageManager;

    private FileDetailsSharingFragmentBinding binding;

    @Inject UserAccountManager accountManager;

    public static FileDetailSharingFragment newInstance(OCFile file, User user) {
        FileDetailSharingFragment fragment = new FileDetailSharingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(ARG_FILE);
            user = savedInstanceState.getParcelable(ARG_USER);
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                file = getArguments().getParcelable(ARG_FILE);
                user = getArguments().getParcelable(ARG_USER);
            }
        }

        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }

        if (user == null) {
            throw new IllegalArgumentException("Account may not be null");
        }

        fileActivity = (FileActivity) getActivity();

        if (fileActivity == null) {
            throw new IllegalArgumentException("FileActivity may not be null");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshCapabilitiesFromDB();
        refreshSharesFromDB();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FileDetailsSharingFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        fileOperationsHelper = fileActivity.getFileOperationsHelper();
        fileDataStorageManager = fileActivity.getStorageManager();

        AccountManager accountManager = AccountManager.get(getContext());
        String userId = accountManager.getUserData(user.toPlatformAccount(),
                                                   com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

        binding.sharesList.setAdapter(new ShareeListAdapter(fileActivity,
                                                            new ArrayList<>(),
                                                            this,
                                                            userId,
                                                            user));
        binding.sharesList.setLayoutManager(new LinearLayoutManager(getContext()));

        setupView();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof FileActivity)) {
            throw new IllegalArgumentException("Calling activity must be of type FileActivity");
        }
    }

    private void setupView() {
        setShareWithYou();

        FileDetailSharingFragmentHelper.setupSearchView(
            (SearchManager) fileActivity.getSystemService(Context.SEARCH_SERVICE),
            binding.searchView,
            fileActivity.getComponentName());
        ThemeToolbarUtils.themeSearchView(binding.searchView, requireContext());

        if (file.canReshare()) {
            binding.searchView.setQueryHint(getResources().getString(R.string.share_search));
        } else {
            binding.searchView.setQueryHint(getResources().getString(R.string.reshare_not_allowed));
            binding.searchView.setInputType(InputType.TYPE_NULL);
            disableSearchView(binding.searchView);
        }
    }

    private void disableSearchView(View view) {
        view.setEnabled(false);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;

            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                disableSearchView(viewGroup.getChildAt(i));
            }
        }
    }

    private void setShareWithYou() {
        if (accountManager.userOwnsFile(file, user)) {
            binding.sharedWithYouContainer.setVisibility(View.GONE);
        } else {
            binding.sharedWithYouUsername.setText(
                String.format(getString(R.string.shared_with_you_by), file.getOwnerDisplayName()));
            DisplayUtils.setAvatar(user,
                                   file.getOwnerId(),
                                   this,
                                   getResources().getDimension(
                                       R.dimen.file_list_item_avatar_icon_radius),
                                   getResources(),
                                   binding.sharedWithYouAvatar,
                                   getContext());
            binding.sharedWithYouAvatar.setVisibility(View.VISIBLE);

            String note = file.getNote();

            if (!TextUtils.isEmpty(note)) {
                binding.sharedWithYouNote.setText(file.getNote());
                binding.sharedWithYouNoteContainer.setVisibility(View.VISIBLE);
            } else {
                binding.sharedWithYouNoteContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void copyInternalLink() {
        OwnCloudAccount account = accountManager.getCurrentOwnCloudAccount();

        if (account == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_url));
            return;
        }

        FileDisplayActivity.showShareLinkDialog(fileActivity, file, createInternalLink(account, file));
    }

    private String createInternalLink(OwnCloudAccount account, OCFile file) {
        return account.getBaseUri() + "/index.php/f/" + file.getLocalId();
    }

    @Override
    public void createPublicShareLink() {
        if (capabilities != null && (capabilities.getFilesSharingPublicPasswordEnforced().isTrue() ||
            capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue())) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink(true,
                                           capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue());

        } else {
            // create without password if not enforced by server or we don't know if enforced;
            fileOperationsHelper.shareFileViaPublicShare(file, null);
        }
    }

    private void showSendLinkTo(OCShare publicShare) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(publicShare.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                FileDisplayActivity.showShareLinkDialog(fileActivity, file, publicShare.getShareLink());
            }
        }
    }

    public void copyLink(OCShare share) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(share.getShareLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                ClipboardUtil.copyToClipboard(getActivity(), share.getShareLink());
            }
        }
    }

    @Override
    public void showUserOverflowMenu(OCShare share, ImageView overflowMenu) {
        // use grey as fallback for elements where custom theming is not available
        if (ThemeUtils.themingEnabled(requireContext())) {
            requireContext().getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }
        PopupMenu popup = new PopupMenu(requireContext(), overflowMenu);
        popup.inflate(R.menu.item_user_sharing_settings);
        prepareUserOptionsMenu(popup.getMenu(), share);
        popup.setOnMenuItemClickListener(item -> userOptionsItemSelected(popup.getMenu(), item, share));
        popup.show();
    }

    /**
     * Updates the sharee's menu with the current permissions of the {@link OCShare}
     *
     * @param menu  the menu of the sharee/shared file
     * @param share the shared file
     */
    @VisibleForTesting
    public void prepareUserOptionsMenu(Menu menu, OCShare share) {
        MenuItem allowEditingItem = menu.findItem(R.id.allow_editing);
        MenuItem allowCreatingItem = menu.findItem(R.id.allow_creating);
        MenuItem allowDeletingItem = menu.findItem(R.id.allow_deleting);
        MenuItem expirationDateItem = menu.findItem(R.id.action_expiration_date);
        MenuItem reshareItem = menu.findItem(R.id.allow_resharing);

        allowEditingItem.setChecked(canEdit(share));

        if (isReshareForbidden(share)) {
            reshareItem.setVisible(false);
        }
        reshareItem.setChecked(canReshare(share));

        if (file.isFolder() || share.isFolder()) {
            allowCreatingItem.setChecked(canCreate(share));
            allowDeletingItem.setChecked(canDelete(share));
        } else {
            allowCreatingItem.setVisible(false);
            allowDeletingItem.setVisible(false);
        }

        if (!capabilities.getVersion().isNewerOrEqual(OwnCloudVersion.nextcloud_18)) {
            expirationDateItem.setVisible(false);
        }

        SharingMenuHelper.setupExpirationDateMenuItem(menu.findItem(R.id.action_expiration_date),
                                                      share.getExpirationDate(),
                                                      getResources());
    }

    public void showLinkOverflowMenu(OCShare publicShare, ImageView overflowMenuShareLink) {
        if (ThemeUtils.themingEnabled(requireContext())) {
            // use grey as fallback for elements where custom theming is not available
            requireContext().getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }

        PopupMenu popup = new PopupMenu(requireContext(), overflowMenuShareLink);
        if (ShareType.EMAIL == publicShare.getShareType()) {
            popup.inflate(R.menu.fragment_file_detail_sharing_email_link);
        } else {
            popup.inflate(R.menu.fragment_file_detail_sharing_public_link);
        }
        prepareLinkOptionsMenu(popup.getMenu(), publicShare);
        popup.setOnMenuItemClickListener(menuItem -> linkOptionsItemSelected(menuItem, publicShare));
        popup.show();
    }

    @VisibleForTesting
    public void prepareLinkOptionsMenu(Menu menu, OCShare publicShare) {
        if (publicShare.isFolder()) {
            menu.setGroupVisible(R.id.folder_permission, true);
            menu.findItem(R.id.allow_editing).setVisible(false);

            // read only / allow upload and editing / file drop
            if (isUploadAndEditingAllowed(publicShare)) {
                menu.findItem(R.id.link_share_allow_upload_and_editing).setChecked(true);
            } else if (isFileDrop(publicShare)) {
                menu.findItem(R.id.link_share_file_drop).setChecked(true);
            } else if (isReadOnly(publicShare)) {
                menu.findItem(R.id.link_share_read_only).setChecked(true);
            }
        } else {
            menu.setGroupVisible(R.id.folder_permission, false);
            menu.findItem(R.id.allow_editing).setVisible(true);

            if (publicShare.getPermissions() > PERMISSION_EDITING_ALLOWED) {
                menu.findItem(R.id.allow_editing).setChecked(true);
            } else {
                menu.findItem(R.id.allow_editing).setChecked(false);
            }
        }

        Resources res = requireContext().getResources();
        SharingMenuHelper.setupHideFileDownload(menu.findItem(R.id.action_hide_file_download),
                                                publicShare.isHideFileDownload(),
                                                isFileDrop(publicShare));

        SharingMenuHelper.setupPasswordMenuItem(menu.findItem(R.id.action_password),
                                                publicShare.isPasswordProtected());

        SharingMenuHelper.setupExpirationDateMenuItem(menu.findItem(R.id.action_share_expiration_date),
                                                      publicShare.getExpirationDate(),
                                                      res);
    }

    @VisibleForTesting
    public boolean isUploadAndEditingAllowed(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & MAXIMUM_PERMISSIONS_FOR_FOLDER) == MAXIMUM_PERMISSIONS_FOR_FOLDER;
    }

    @VisibleForTesting
    public boolean isReadOnly(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == READ_PERMISSION_FLAG;
    }

    @VisibleForTesting
    public boolean isFileDrop(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == CREATE_PERMISSION_FLAG;
    }

    private boolean userOptionsItemSelected(Menu menu, MenuItem item, OCShare share) {
        int itemId = item.getItemId();

        if (itemId == R.id.allow_editing || itemId == R.id.allow_creating || itemId == R.id.allow_deleting || itemId == R.id.allow_resharing) {
            item.setChecked(!item.isChecked());
            share.setPermissions(updatePermissionsToShare(share,
                                                          menu.findItem(R.id.allow_resharing).isChecked(),
                                                          menu.findItem(R.id.allow_editing).isChecked(),
                                                          menu.findItem(R.id.allow_creating).isChecked(),
                                                          menu.findItem(R.id.allow_deleting).isChecked()));
            return true;
        } else if (itemId == R.id.action_unshare) {
            unshareWith(share);
            ShareeListAdapter adapter = (ShareeListAdapter) binding.sharesList.getAdapter();
            if (adapter == null) {
                DisplayUtils.showSnackMessage(getView(), getString(R.string.failed_update_ui));
                return true;
            }
            adapter.remove(share);

            return true;
        } else if (itemId == R.id.action_expiration_date) {
            ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment
                .newInstance(share, share.getExpirationDate());
            dialog.show(fileActivity.getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG);
            return true;
        } else if (itemId == R.id.action_share_send_note) {
            NoteDialogFragment dialog = NoteDialogFragment.newInstance(share);
            dialog.show(fileActivity.getSupportFragmentManager(), NoteDialogFragment.NOTE_FRAGMENT);
            return true;
        }

        return true;
    }

    public boolean linkOptionsItemSelected(MenuItem item, OCShare publicShare) {
        int itemId = item.getItemId();

        if (itemId == R.id.link_share_read_only) {
            item.setChecked(true);
            fileOperationsHelper.setPermissionsToShare(publicShare, READ_PERMISSION_FLAG);
            return true;
        } else if (itemId == R.id.link_share_allow_upload_and_editing) {
            item.setChecked(true);
            if (publicShare.isFolder()) {
                fileOperationsHelper.setPermissionsToShare(publicShare, MAXIMUM_PERMISSIONS_FOR_FOLDER);
            } else {
                fileOperationsHelper.setPermissionsToShare(publicShare, MAXIMUM_PERMISSIONS_FOR_FILE);
            }
            return true;
        } else if (itemId == R.id.link_share_file_drop) {
            item.setChecked(true);
            fileOperationsHelper.setPermissionsToShare(publicShare, CREATE_PERMISSION_FLAG);
            return true;
        } else if (itemId == R.id.allow_editing) {
            if (file.isSharedViaLink()) {
                item.setChecked(!item.isChecked());
                fileOperationsHelper.setUploadPermissionsToPublicShare(publicShare, item.isChecked());
            }
            return true;
        } else if (itemId == R.id.action_hide_file_download) {
            item.setChecked(!item.isChecked());
            fileOperationsHelper.setHideFileDownloadPermissionsToPublicShare(publicShare, item.isChecked());
            return true;
        } else if (itemId == R.id.action_password) {
            requestPasswordForShare(publicShare,
                                    capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue());
            return true;
        } else if (itemId == R.id.action_share_expiration_date) {
            ExpirationDatePickerDialogFragment expirationDialog = ExpirationDatePickerDialogFragment
                .newInstance(publicShare, publicShare.getExpirationDate());
            expirationDialog.show(fileActivity.getSupportFragmentManager(),
                                  ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG);
            return true;
        } else if (itemId == R.id.action_share_send_link) {
            if (file.isSharedViaLink() && !TextUtils.isEmpty(publicShare.getShareLink())) {
                FileDisplayActivity.showShareLinkDialog(fileActivity, file, publicShare.getShareLink());
            } else {
                showSendLinkTo(publicShare);
            }
            return true;
        } else if (itemId == R.id.action_share_send_note) {
            NoteDialogFragment noteDialog = NoteDialogFragment.newInstance(publicShare);
            noteDialog.show(fileActivity.getSupportFragmentManager(), NoteDialogFragment.NOTE_FRAGMENT);
            return true;
        } else if (itemId == R.id.action_edit_label) {
            RenamePublicShareDialogFragment renameDialog = RenamePublicShareDialogFragment.newInstance(publicShare);
            renameDialog.show(fileActivity.getSupportFragmentManager(),
                              RenamePublicShareDialogFragment.RENAME_PUBLIC_SHARE_FRAGMENT);
            return true;
        } else if (itemId == R.id.action_unshare) {
            fileOperationsHelper.unshareShare(file, publicShare);
            return true;
        } else if (itemId == R.id.action_add_another_public_share_link) {
            createPublicShareLink();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the UI after the result of an update operation on the edited {@link OCFile}.
     *
     * @param result {@link RemoteOperationResult} of an update on the edited {@link OCFile} sharing information.
     * @param file   the edited {@link OCFile}
     */
    public void onUpdateShareInformation(RemoteOperationResult result, OCFile file) {
        this.file = file;

        if (result.isSuccess()) {
            refreshUiFromDB();
        } else {
            setupView();
        }
    }

    /**
     * Get {@link OCShare} instance from DB and updates the UI.
     */
    private void refreshUiFromDB() {
        refreshSharesFromDB();
        // Updates UI with new state
        setupView();
    }

    private void unshareWith(OCShare share) {
        fileOperationsHelper.unshareShare(file, share);
    }

    private int updatePermissionsToShare(OCShare share,
                                         boolean canReshare,
                                         boolean canEdit,
                                         boolean canEditCreate,
                                         boolean canEditDelete) {
        SharePermissionsBuilder spb = new SharePermissionsBuilder();
        spb.setSharePermission(canReshare);

        if (file.isFolder()) {
            spb.setCreatePermission(canEditCreate)
                .setDeletePermission(canEditDelete);
        } else {
            spb.setUpdatePermission(canEdit);
        }
        int permissions = spb.build();

        fileOperationsHelper.setPermissionsToShare(share, permissions);

        return permissions;
    }

    /**
     * Starts a dialog that requests a password to the user to protect a share link.
     *
     * @param createShare    When 'true', the request for password will be followed by the creation of a new public
     *                       link; when 'false', a public share is assumed to exist, and the password is bound to it.
     * @param askForPassword if true, password is optional
     */
    public void requestPasswordForShareViaLink(boolean createShare, boolean askForPassword) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(file,
                                                                                     createShare,
                                                                                     askForPassword);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    @Override
    public void requestPasswordForShare(OCShare share, boolean askForPassword) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(share, askForPassword);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    /**
     * Get known server capabilities from DB
     */
    public void refreshCapabilitiesFromDB() {
        capabilities = fileDataStorageManager.getCapability(user.getAccountName());
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     * Takes into account server capabilities before reading database.
     */
    public void refreshSharesFromDB() {
        ShareeListAdapter adapter = (ShareeListAdapter) binding.sharesList.getAdapter();

        if (adapter == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_shares));
            return;
        }
        adapter.getShares().clear();

        // to show share with users/groups info
        List<OCShare> shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(),
                                                                            user.toPlatformAccount().name);

        adapter.addShares(shares);

        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities) || !file.canReshare()) {
            return;
        }

        // Get public share
        List<OCShare> publicShares = fileDataStorageManager.getSharesByPathAndType(file.getRemotePath(),
                                                                                   ShareType.PUBLIC_LINK,
                                                                                   "");


        if (publicShares.isEmpty() && containsNoNewPublicShare(adapter.getShares())) {
            publicShares.add(new OCShare().setShareType(ShareType.NEW_PUBLIC_LINK));
        } else {
            adapter.removeNewPublicShare();
        }

        adapter.addShares(publicShares);
    }


    private boolean containsNoNewPublicShare(List<OCShare> shares) {
        for (OCShare share : shares) {
            if (share.getShareType() == ShareType.NEW_PUBLIC_LINK) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        binding.sharedWithYouAvatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }

    private boolean isReshareForbidden(OCShare share) {
        return ShareType.FEDERATED.equals(share.getShareType()) ||
            capabilities != null && capabilities.getFilesSharingResharing().isFalse();
    }

    private boolean canEdit(OCShare share) {
        return (share.getPermissions() &
            (CREATE_PERMISSION_FLAG | UPDATE_PERMISSION_FLAG | DELETE_PERMISSION_FLAG)) > 0;
    }

    private boolean canCreate(OCShare share) {
        return (share.getPermissions() & CREATE_PERMISSION_FLAG) > 0;
    }

    private boolean canDelete(OCShare share) {
        return (share.getPermissions() & DELETE_PERMISSION_FLAG) > 0;
    }

    private boolean canReshare(OCShare share) {
        return (share.getPermissions() & SHARE_PERMISSION_FLAG) > 0;
    }

    @VisibleForTesting
    public void search(String query) {
        SearchView searchView = getView().findViewById(R.id.searchView);
        searchView.setQuery(query, true);
    }

    public OCFile getFile() {
        return file;
    }
}
