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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
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
import com.owncloud.android.ui.adapter.PublicShareInterface;
import com.owncloud.android.ui.adapter.PublicShareListAdapter;
import com.owncloud.android.ui.adapter.ShareeListAdapter;
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
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FileDetailSharingFragment extends Fragment implements ShareeListAdapter.ShareeListAdapterListener,
    DisplayUtils.AvatarGenerationListener,
    PublicShareInterface,
    Injectable {

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    public static final int PERMISSION_EDITING_ALLOWED = 17;

    // to show share with users/groups info
    private List<OCShare> shares;

    private OCFile file;
    private User user;
    private OCCapability capabilities;

    private FileOperationsHelper fileOperationsHelper;
    private FileActivity fileActivity;
    private FileDataStorageManager fileDataStorageManager;

    private Unbinder unbinder;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.shareUsersList)
    RecyclerView usersList;

    @BindView(R.id.publicShareList)
    RecyclerView publicShareList;

    @BindView(R.id.new_public_share)
    View addPublicShare;

    @BindView(R.id.shared_with_you_container)
    LinearLayout sharedWithYouContainer;

    @BindView(R.id.shared_with_you_avatar)
    ImageView sharedWithYouAvatar;

    @BindView(R.id.shared_with_you_username)
    TextView sharedWithYouUsername;

    @BindView(R.id.shared_with_you_note_container)
    View sharedWithYouNoteContainer;

    @BindView(R.id.shared_with_you_note)
    TextView sharedWithYouNote;

    @BindView(R.id.copy_internal_link_icon)
    ImageView internalLinkIcon;

    @BindView(R.id.shareInternalLinkText)
    TextView internalLinkText;

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
        refreshPublicShareFromDB();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_details_sharing_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        fileOperationsHelper = fileActivity.getFileOperationsHelper();
        fileDataStorageManager = fileActivity.getStorageManager();

        setupView();

        // todo extract
        internalLinkIcon.getBackground().setColorFilter(getResources().getColor(R.color.grey_db),
                                                        PorterDuff.Mode.SRC_IN);
        internalLinkIcon.getDrawable().mutate().setColorFilter(getResources().getColor(R.color.black),
                                                               PorterDuff.Mode.SRC_IN);

        if (file.isFolder()) {
            internalLinkText.setText(getString(R.string.share_internal_link_to_folder_text));
        } else {
            internalLinkText.setText(getString(R.string.share_internal_link_to_file_text));
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
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
            (SearchManager) fileActivity.getSystemService(Context.SEARCH_SERVICE), searchView,
            fileActivity.getComponentName());
        ThemeUtils.themeSearchView(searchView, requireContext());

        if (file.canReshare()) {
            setShareWithUserInfo();
        } else {
            searchView.setQueryHint(getResources().getString(R.string.reshare_not_allowed));
            searchView.setInputType(InputType.TYPE_NULL);
            disableSearchView(searchView);
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
            sharedWithYouContainer.setVisibility(View.GONE);
        } else {
            sharedWithYouUsername.setText(
                String.format(getString(R.string.shared_with_you_by), file.getOwnerDisplayName()));
            DisplayUtils.setAvatar(user, file.getOwnerId(), this, getResources().getDimension(
                R.dimen.file_list_item_avatar_icon_radius), getResources(), sharedWithYouAvatar,
                getContext());
            sharedWithYouAvatar.setVisibility(View.VISIBLE);

            String note = file.getNote();

            if (!TextUtils.isEmpty(note)) {
                sharedWithYouNote.setText(file.getNote());
                sharedWithYouNoteContainer.setVisibility(View.VISIBLE);
            } else {
                sharedWithYouNoteContainer.setVisibility(View.GONE);
            }
        }
    }

    private void setShareWithUserInfo() {
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(), account.name);
        if (shares.size() > 0) {
            AccountManager accountManager = AccountManager.get(getContext());
            String userId = accountManager.getUserData(user.toPlatformAccount(),
                                                       com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(new ShareeListAdapter(fileActivity.getSupportFragmentManager(),
                                                       fileActivity,
                                                       shares,
                                                       user.toPlatformAccount(),
                                                       file,
                                                       this,
                                                       userId));
            usersList.setLayoutManager(new LinearLayoutManager(getContext()));
            usersList.addItemDecoration(new SimpleListItemDividerDecoration(getContext()));
        } else {
            usersList.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.copy_internal_container)
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
                FileDisplayActivity.showShareLinkDialog(fileActivity, file, file.getPublicLink());
            }
        }
    }

    public void copyLink(OCShare share) {
        if (file.isSharedViaLink()) {
            if (TextUtils.isEmpty(file.getPublicLink())) {
                fileOperationsHelper.getFileWithLink(file);
            } else {
                ClipboardUtil.copyToClipboard(getActivity(), file.getPublicLink());
            }
        }
    }

    public void showLinkOverflowMenu(OCShare publicShare, ImageView overflowMenuShareLink) {
        if (ThemeUtils.themingEnabled(requireContext())) {
            // use grey as fallback for elements where custom theming is not available
            requireContext().getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }

        PopupMenu popup = new PopupMenu(requireContext(), overflowMenuShareLink);
        popup.inflate(R.menu.fragment_file_detail_sharing_link);
        prepareOptionsMenu(popup.getMenu(), publicShare);
        popup.setOnMenuItemClickListener(menuItem -> optionsItemSelected(menuItem, publicShare));
        popup.show();
    }

    private void prepareOptionsMenu(Menu menu, OCShare publicShare) {
        Resources res = requireContext().getResources();
        SharingMenuHelper.setupHideFileListingMenuItem(menu.findItem(R.id.action_hide_file_listing),
                                                       file.isFolder(),
                                                       menu.findItem(R.id.action_allow_editing).isChecked(),
                                                       publicShare.getPermissions());

        SharingMenuHelper.setupHideFileDownload(menu.findItem(R.id.action_hide_file_download),
                                                publicShare.isHideFileDownload(),
                                                capabilities);

        SharingMenuHelper.setupPasswordMenuItem(menu.findItem(R.id.action_password),
                                                publicShare.isPasswordProtected());

        SharingMenuHelper.setupExpirationDateMenuItem(menu.findItem(R.id.action_share_expiration_date),
                                                      publicShare.getExpirationDate(),
                                                      res);

        menu.findItem(R.id.action_share_send_note).setVisible(capabilities.getVersion().isNoteOnShareSupported());

        if (publicShare.getPermissions() > PERMISSION_EDITING_ALLOWED) {
            menu.findItem(R.id.action_allow_editing).setChecked(true);
        } else {
            menu.findItem(R.id.action_allow_editing).setChecked(false);
        }
    }

    public boolean optionsItemSelected(MenuItem item, OCShare publicShare) {
        switch (item.getItemId()) {
            case R.id.action_allow_editing:
                if (file.isSharedViaLink()) {
                    item.setChecked(!item.isChecked());
                    fileOperationsHelper.setUploadPermissionsToShare(file,
                                                                     item.isChecked());
                }
                return true;
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
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment
                    .newInstance(file, publicShare.getExpirationDate());
                dialog.show(fileActivity.getSupportFragmentManager(),
                            ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG);
                return true;
            }
            case R.id.action_share_send_link: {
                if (file.isSharedViaLink() && !TextUtils.isEmpty(file.getPublicLink())) {
                    FileDisplayActivity.showShareLinkDialog(fileActivity, file, file.getPublicLink());
                } else {
                    showSendLinkTo();
                }
                return true;
            }
            case R.id.action_share_send_note:
                NoteDialogFragment dialog = NoteDialogFragment.newInstance(publicShare);
                dialog.show(fileActivity.getSupportFragmentManager(), NoteDialogFragment.NOTE_FRAGMENT);
                return true;
            case R.id.action_add_another_public_share_link:
                createShareLink();
                return true;
            case R.id.action_unshare:
                fileOperationsHelper.unshareShare(file, publicShare);
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
        // Updates UI with new state
        setupView();
    }

    @Override
    public void unshareWith(OCShare share) {
        fileOperationsHelper.unshareShare(file, share);
    }

    @Override
    public int updatePermissionsToShare(OCShare share,
                                        boolean canReshare,
                                        boolean canEdit,
                                        boolean canEditCreate,
                                        boolean canEditChange,
                                        boolean canEditDelete) {
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
        capabilities = fileDataStorageManager.getCapability(user.getAccountName());
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     *
     * Takes into account server capabilities before reading database.
     */
    public void refreshPublicShareFromDB() {
        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities) || !file.canReshare()) {
            publicShareList.setVisibility(View.GONE);
            return;
        }

        // Get public share
        List<OCShare> shares = fileDataStorageManager.getSharesByPathAndType(file.getRemotePath(),
                                                                             ShareType.PUBLIC_LINK,
                                                                             "");

        if (shares.isEmpty()) {
            addPublicShare.setVisibility(View.VISIBLE);
            publicShareList.setVisibility(View.GONE);
            ImageView icon = requireView().findViewById(R.id.copy_internal_link_icon);
            icon.getBackground().setColorFilter(requireContext()
                                                    .getResources()
                                                    .getColor(R.color.primary_button_background_color),
                                                PorterDuff.Mode.SRC_IN);
            icon.getDrawable().mutate().setColorFilter(requireContext().getResources().getColor(R.color.black),
                                                       PorterDuff.Mode.SRC_IN);
            requireView().findViewById(R.id.add_new_public_share_link).setOnClickListener(v -> createShareLink());
        } else {
            addPublicShare.setVisibility(View.GONE);
            publicShareList.setVisibility(View.VISIBLE);
            publicShareList.setAdapter(new PublicShareListAdapter(getContext(), shares, this));
            publicShareList.setLayoutManager(new LinearLayoutManager(getContext()));
            publicShareList.addItemDecoration(new SimpleListItemDividerDecoration(getContext()));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
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
