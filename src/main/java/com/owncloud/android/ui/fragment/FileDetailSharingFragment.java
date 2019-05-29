/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.decoration.SimpleListItemDividerDecoration;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.NoteDialogFragment;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.utils.ClipboardUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FileDetailSharingFragment extends Fragment implements UserListAdapter.ShareeListAdapterListener,
    DisplayUtils.AvatarGenerationListener, Injectable {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // to show share with users/groups info
    private List<OCShare> shares;

    private OCFile file;
    private Account account;
    private OCCapability capabilities;
    private OCShare publicShare;

    private FileOperationsHelper fileOperationsHelper;
    private FileDisplayActivity fileDisplayActivity;
    private FileDataStorageManager fileDataStorageManager;

    private Unbinder unbinder;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.shareUsersList)
    RecyclerView usersList;

    @BindView(R.id.shareNoUsers)
    TextView noList;

    @BindView(R.id.share_by_link)
    AppCompatCheckBox shareByLink;

    @BindView(R.id.share_link_copy_icon)
    ImageView shareLinkCopyIcon;

    @BindView(R.id.overflow_menu_share_link)
    ImageView overflowMenuShareLink;

    @BindView(R.id.share_by_link_allow_editing)
    AppCompatCheckBox shareByLinkAllowEditing;

    @BindView(R.id.share_by_link_container)
    LinearLayout shareByLinkContainer;

    @BindView(R.id.shared_with_you_container)
    LinearLayout sharedWithYouContainer;

    @BindView(R.id.shared_with_you_avatar)
    ImageView sharedWithYouAvatar;

    @BindView(R.id.shared_with_you_username)
    TextView sharedWithYouUsername;

    @BindView(R.id.shared_with_you_note)
    TextView sharedWithYouNote;

    @BindView(R.id.copy_internal_link_icon)
    ImageView internalLinkIcon;

    @Inject UserAccountManager accountManager;

    public static FileDetailSharingFragment newInstance(OCFile file, Account account) {
        FileDetailSharingFragment fragment = new FileDetailSharingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        } else {
            Bundle arguments = getArguments();

            if (arguments != null) {
                file = getArguments().getParcelable(ARG_FILE);
                account = getArguments().getParcelable(ARG_ACCOUNT);
            }
        }

        if (file == null) {
            throw new IllegalArgumentException("File may not be null");
        }

        if (account == null) {
            throw new IllegalArgumentException("Account may not be null");
        }

        fileDisplayActivity = (FileDisplayActivity) getActivity();

        if (fileDisplayActivity == null) {
            throw new IllegalArgumentException("FileActivity may not be null");
        }

        fileOperationsHelper = fileDisplayActivity.getFileOperationsHelper();
        fileDataStorageManager = fileDisplayActivity.getStorageManager();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshCapabilitiesFromDB();
        refreshPublicShareFromDB();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_details_sharing_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (fileDataStorageManager == null) {
            fileDataStorageManager = new FileDataStorageManager(account, fileDisplayActivity.getContentResolver());
        }

        setupView();

        // todo extract
        internalLinkIcon.getBackground().setColorFilter(getResources().getColor(R.color.grey_db),
                                                        PorterDuff.Mode.SRC_IN);
        internalLinkIcon.getDrawable().mutate().setColorFilter(getResources().getColor(R.color.black),
                                                               PorterDuff.Mode.SRC_IN);



        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(getActivity() instanceof FileActivity)) {
            throw new IllegalArgumentException("Calling activity must be of type FileActivity");
        }
    }

    private void setupView() {
        setShareWithYou();

        if (file.canReshare()) {
            setShareByLinkInfo(file.isSharedViaLink());
            setShareWithUserInfo();
            FileDetailSharingFragmentHelper.setupSearchView(
                (SearchManager) fileDisplayActivity.getSystemService(Context.SEARCH_SERVICE), searchView,
                fileDisplayActivity.getComponentName());
            ThemeUtils.themeSearchView(getContext(), searchView, false);
        } else {
            searchView.setVisibility(View.GONE);
            shareByLinkContainer.setVisibility(View.GONE);
            noList.setText(R.string.reshare_not_allowed);
        }
    }

    /**
     * Updates Share by link UI
     *
     * @param isShareByLink flag is share by link is enable
     */
    public void setShareByLinkInfo(boolean isShareByLink) {
        shareByLink.setChecked(isShareByLink);
        if (isShareByLink) {
            shareLinkCopyIcon.setVisibility(View.VISIBLE);
        } else {
            shareLinkCopyIcon.setVisibility(View.INVISIBLE);
        }
        int accentColor = ThemeUtils.primaryAccentColor(getContext());
        ThemeUtils.tintCheckbox(shareByLink, accentColor);
        ThemeUtils.tintCheckbox(shareByLinkAllowEditing, accentColor);
        setLinkDetailVisible(isShareByLink);
    }

    private void setLinkDetailVisible(boolean visible) {
        if (visible) {
                shareByLinkAllowEditing.setVisibility(View.VISIBLE);
            overflowMenuShareLink.setVisibility(View.VISIBLE);
        } else {
            shareByLinkAllowEditing.setVisibility(View.INVISIBLE);
            overflowMenuShareLink.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Update Share With data
     */
    public void setShareWithUserInfo() {
        // Get Users and Groups
        shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(), account.name);

        // Update list of users/groups
        updateListOfUserGroups();
    }

    private void setShareWithYou() {
        if (accountManager.accountOwnsFile(file, account)) {
            sharedWithYouContainer.setVisibility(View.GONE);
        } else {
            sharedWithYouUsername.setText(
                String.format(getString(R.string.shared_with_you_by), file.getOwnerDisplayName()));

            DisplayUtils.setAvatar(account, file.getOwnerId(), this, getResources().getDimension(
                R.dimen.file_list_item_avatar_icon_radius), getResources(), sharedWithYouAvatar,
                getContext());
            sharedWithYouAvatar.setVisibility(View.VISIBLE);

            String note = file.getNote();

            if (!TextUtils.isEmpty(note)) {
                sharedWithYouNote.setText(file.getNote());
                sharedWithYouNote.setVisibility(View.VISIBLE);
            } else {
                sharedWithYouNote.setVisibility(View.GONE);
            }
        }
    }

    private void updateListOfUserGroups() {
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed

        if (shares.size() > 0) {
            AccountManager accountManager = AccountManager.get(getContext());
            String userId = accountManager.getUserData(account,
                com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(new UserListAdapter(fileDisplayActivity.getSupportFragmentManager(),
                fileDisplayActivity, shares, account, file, this, userId));
            usersList.setLayoutManager(new LinearLayoutManager(getContext()));
            usersList.addItemDecoration(new SimpleListItemDividerDecoration(getContext()));
            noList.setVisibility(View.GONE);
        } else {
            usersList.setVisibility(View.GONE);
            noList.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.share_by_link)
    public void toggleShareByLink() {
        if (shareByLink.isChecked()) {
            createShareLink();
        } else {
            fileOperationsHelper.unshareFileViaLink(file);
        }
    }

    @OnClick(R.id.copy_internal_container)
    public void copyInternalLink() {
        OwnCloudAccount account = accountManager.getCurrentOwnCloudAccount();

        if (account == null) {
            DisplayUtils.showSnackMessage(getView(), getString(R.string.could_not_retrieve_url));
            return;
        }

        FileDisplayActivity.showShareLinkDialog(fileDisplayActivity, createInternalLink(account, file));
    }

    private String createInternalLink(OwnCloudAccount account, OCFile file) {
        return account.getBaseUri() + "/index.php/f/" + file.getLocalId();
    }

    private void createShareLink() {
        if (capabilities != null && (capabilities.getFilesSharingPublicPasswordEnforced().isTrue() ||
            capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue())) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink(true,
                                           capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue());

        } else {
            // create without password if not enforced by server or we don't know if enforced;
            fileOperationsHelper.shareFileViaLink(file, null);
        }
    }

    private void showSendLinkTo() {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(file.getPublicLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                FileDisplayActivity.showShareLinkDialog(fileDisplayActivity, file.getPublicLink());
            }
        }
    }

    @OnClick({R.id.share_link_copy_icon})
    public void copyLinkToClipboard() {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(file.getPublicLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                ClipboardUtil.copyToClipboard(getActivity(), file.getPublicLink());
            }
        }
    }

    @OnClick(R.id.share_by_link_allow_editing)
    public void toggleShareLinkAllowEditing() {
        if (file.isSharedViaLink()) {
            fileOperationsHelper.setUploadPermissionsToShare(file, shareByLinkAllowEditing.isChecked());
        }
    }

    @OnClick(R.id.overflow_menu_share_link)
    public void showLinkOverflowMenu() {
        Context context = getContext();
        if (context != null && ThemeUtils.themingEnabled(context)) {
            // use grey as fallback for elements where custom theming is not available
            context.getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        } else {
            context = getActivity();
        }

        PopupMenu popup = new PopupMenu(context, overflowMenuShareLink);
        popup.inflate(R.menu.file_detail_sharing_link_menu);
        prepareOptionsMenu(popup.getMenu());
        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    private void prepareOptionsMenu(Menu menu) {
        Resources res = getResources();
        SharingMenuHelper.setupHideFileListingMenuItem(
                menu.findItem(R.id.action_hide_file_listing),
                file.isFolder(),
                shareByLinkAllowEditing.isChecked(),
                publicShare.getPermissions()
        );
        SharingMenuHelper.setupHideFileDownload(menu.findItem(R.id.action_hide_file_download),
            publicShare.isHideFileDownload(), capabilities);
        SharingMenuHelper.setupPasswordMenuItem(
                menu.findItem(R.id.action_password),
                publicShare.isPasswordProtected()
        );
        SharingMenuHelper.setupExpirationDateMenuItem(
                menu.findItem(R.id.action_share_expiration_date),
                publicShare.getExpirationDate(),
                res
        );

        menu.findItem(R.id.action_share_send_note).setVisible(capabilities.getVersion().isNoteOnShareSupported());
    }

    private boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_hide_file_listing: {
                item.setChecked(!item.isChecked());
                if (capabilities.getFilesFileDrop().isTrue()) {
                    setHideFileListingPermissionsToShare(publicShare, item.isChecked());
                } else {
                    // not supported in ownCloud
                    showNotSupportedByOcMessage();
                }
                return true;
            }
            case R.id.action_hide_file_download:
                item.setChecked(!item.isChecked());
                setHideFileDownloadPermissionToShare(file, item.isChecked());

                return true;
            case R.id.action_password: {
                requestPasswordForShareViaLink(false,
                                               capabilities.getFilesSharingPublicAskForOptionalPassword().isTrue());
                return true;
            }
            case R.id.action_share_expiration_date: {
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.newInstance(file, -1);
                dialog.show(
                    fileDisplayActivity.getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );
                return true;
            }
            case R.id.action_share_send_link: {
                if(shareByLink.isChecked() && file.isSharedViaLink() && !TextUtils.isEmpty(file.getPublicLink())) {
                    FileDisplayActivity.showShareLinkDialog(fileDisplayActivity, file.getPublicLink());
                } else {
                    showSendLinkTo();
                }
                return true;
            }
            case R.id.action_share_send_note:
                NoteDialogFragment dialog = NoteDialogFragment.newInstance(publicShare);
                dialog.show(fileDisplayActivity.getSupportFragmentManager(), NoteDialogFragment.NOTE_FRAGMENT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setHideFileListingPermissionsToShare(OCShare share, boolean hideFileListing) {
        fileOperationsHelper.setHideFileListingPermissionsToShare(share, hideFileListing);
    }

    @Override
    public void setHideFileDownloadPermissionToShare(OCFile file, boolean hideFileDownload) {
        fileOperationsHelper.setHideFileDownloadPermissionsToShare(file, hideFileDownload);
    }

    @Override
    public void showNotSupportedByOcMessage() {
        if (getView() != null) {
            Snackbar.make(getView(), R.string.files_drop_not_supported, Snackbar.LENGTH_LONG)
                .setAction(R.string.learn_more, v ->
                    DisplayUtils.startLinkIntent(requireActivity(), R.string.url_server_install))
                    .show();
        }
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
        if (publicShare != null) {
            // Get edited shared by link
            publicShare = fileDataStorageManager.getShareById(publicShare.getId());
        }

        // Updates UI with new state
        setupView();
    }

    @Override
    public void unshareWith(OCShare share) {
        fileOperationsHelper.unshareFileWithUserOrGroup(file, share.getShareType(), share.getShareWith());
    }

    @Override
    public int updatePermissionsToShare(OCShare share, boolean canReshare, boolean canEdit, boolean canEditCreate,
                                        boolean canEditChange, boolean canEditDelete) {
        SharePermissionsBuilder spb = new SharePermissionsBuilder();
        spb.setSharePermission(canReshare);

        if (file.isFolder()) {
            spb.setUpdatePermission(canEditChange)
                    .setCreatePermission(canEditCreate)
                    .setDeletePermission(canEditDelete);
        } else {
            spb.setUpdatePermission(canEdit);
        }
        int permissions = spb.build();

        fileOperationsHelper.setPermissionsToShare(share, permissions);

        return permissions;
    }

    @Override
    public void updateNoteToShare(OCShare share, String note) {
        fileOperationsHelper.updateNoteToShare(share, note);
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
    public void requestPasswordForShare(OCShare share) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(share);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    /**
     * Get known server capabilities from DB
     */
    public void refreshCapabilitiesFromDB() {
        capabilities = fileDataStorageManager.getCapability(account.name);
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     *
     * Takes into account server capabilities before reading database.
     */
    public void refreshPublicShareFromDB() {
        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities) || !file.canReshare()) {
            shareByLinkContainer.setVisibility(View.GONE);
        } else {
            // Get public share
            publicShare = fileDataStorageManager.getFirstShareByPathAndType(file.getRemotePath(),
                ShareType.PUBLIC_LINK, "");

            // Update public share section
            updatePublicShareSection();
        }
    }

    /**
     * Updates in the UI the section about public share with the information
     * in the current public share bound to, if any.
     */
    private void updatePublicShareSection() {
        if (publicShare != null && ShareType.PUBLIC_LINK.equals(publicShare.getShareType())) {
            shareByLink.setChecked(true);

            if (publicShare.getPermissions() > OCShare.READ_PERMISSION_FLAG) {
                shareByLinkAllowEditing.setChecked(true);
            } else {
                shareByLinkAllowEditing.setChecked(false);
            }

            setShareByLinkInfo(true);
        } else {
            setShareByLinkInfo(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(FileActivity.EXTRA_FILE, file);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, account);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        sharedWithYouAvatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }
}
