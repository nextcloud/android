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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FileDetailSharingFragment extends Fragment implements UserListAdapter.ShareeListAdapterListener {
    private static final String TAG = FileDetailSharingFragment.class.getSimpleName();

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    // to show share with users/groups info
    private ArrayList<OCShare> shares;

    private OCFile file;
    private Account account;
    private OCCapability capabilities;
    private OCShare publicShare;

    private Unbinder unbinder;

    @BindView(R.id.searchView)
    SearchView searchView;

    @BindView(R.id.fdshareUsersList)
    ListView usersList;

    @BindView(R.id.fdShareNoUsers)
    TextView noList;

    @BindView(R.id.share_by_link)
    CheckBox shareByLink;

    @BindView(R.id.overflow_menu_share_link)
    ImageView overflowMenuShareLink;

    @BindView(R.id.share_by_link_allow_editing)
    CheckBox shareByLinkAllowEditing;

    @BindView(R.id.share_by_link_container)
    LinearLayout shareByLinkContainer;

    public static FileDetailSharingFragment newInstance(OCFile file, Account account) {
        FileDetailSharingFragment fragment = new FileDetailSharingFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        refreshCapabilitiesFromDB();
        refreshPublicShareFromDB();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        file = getArguments().getParcelable(ARG_FILE);
        account = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        View view = inflater.inflate(R.layout.file_details_sharing_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupView();

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
        setShareByLinkInfo(file.isSharedViaLink());
        setShareWithUserInfo();
        setupSearchView();
    }

    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        // assumes parent activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        // do not iconify the widget; expand it by default
        searchView.setIconifiedByDefault(false);

        // avoid fullscreen with softkeyboard
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log_OC.v(TAG, "onQueryTextSubmit intercepted, query: " + query);
                // return true to prevent the query is processed to be queried;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // leave it for the parent listener in the hierarchy / default behaviour
                return false;
            }
        });
    }

    /**
     * Updates Share by link UI
     *
     * @param isShareByLink flag is share by link is enable
     */
    private void setShareByLinkInfo(boolean isShareByLink) {
        shareByLink.setChecked(isShareByLink);
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
        if (((FileActivity) getActivity()).getStorageManager() != null) {
            FileDataStorageManager fileDataStorageManager = ((FileActivity) getActivity()).getStorageManager();

            shares = fileDataStorageManager.getSharesWithForAFile(file.getRemotePath(), account.name);

            // Update list of users/groups
            updateListOfUserGroups();
        }
    }

    private void updateListOfUserGroups() {
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        UserListAdapter mUserGroupsAdapter = new UserListAdapter(getActivity().getApplicationContext(),
                R.layout.share_user_item, shares, account, file, this);

        if (shares.size() > 0) {
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);
            noList.setVisibility(View.GONE);
            setListViewHeightBasedOnChildren(usersList);
        } else {
            usersList.setVisibility(View.GONE);
            noList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Fix scroll in ListView when the parent is a ScrollView
     */
    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @OnClick(R.id.share_by_link)
    public void toggleShareByLink() {
        if (shareByLink.isChecked()) {
            if (capabilities != null &&
                    capabilities.getFilesSharingPublicPasswordEnforced().isTrue()) {
                // password enforced by server, request to the user before trying to create
                requestPasswordForShareViaLink(true);

            } else {
                // create without password if not enforced by server or we don't know if enforced;
                ((FileActivity) getActivity()).getFileOperationsHelper().shareFileViaLink(file, null);
            }

        } else {
            ((FileActivity) getActivity()).getFileOperationsHelper().unshareFileViaLink(file);
        }
    }

    @OnClick(R.id.share_link_label)
    public void showSendLinkTo() {
        if (file.isSharedViaLink()) {
            ((FileActivity) getActivity()).getFileOperationsHelper().getFileWithLink(file);
        }
    }

    @OnClick(R.id.share_by_link_allow_editing)
    public void toggleShareLinkAllowEditing() {
        if (file.isSharedViaLink()) {
            ((FileActivity) getActivity()).getFileOperationsHelper().setUploadPermissionsToShare(file, shareByLinkAllowEditing.isChecked());
        }
    }

    @OnClick(R.id.overflow_menu_share_link)
    public void showLinkOverflowMenu() {
        PopupMenu popup = new PopupMenu(getActivity(), overflowMenuShareLink);
        popup.inflate(R.menu.file_detail_sharing_link_menu);
        prepareOptionsMenu(popup.getMenu());
        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    private void prepareOptionsMenu(Menu menu) {
        Resources res = getResources();
        setupHideFileListingMenuItem(menu.findItem(R.id.action_share_link_hide_file_listing));
        setupPasswordMenuItem(menu.findItem(R.id.action_share_link_password), res);
        setupExpirationDateMenuItem(menu.findItem(R.id.action_share_link_expiration_date), res);
    }

    private void setupPasswordMenuItem(MenuItem password, Resources res) {
        if (publicShare.isPasswordProtected()) {
            password.setTitle(res.getString(
                    R.string.share_via_link_menu_password_label,
                    res.getString(R.string.share_via_link_password_title)
            ));
        } else {
            password.setTitle(res.getString(
                    R.string.share_via_link_menu_password_label,
                    res.getString(R.string.share_via_link_no_password_title)
            ));
        }
    }

    private void setupHideFileListingMenuItem(MenuItem fileListing) {
        if (!file.isFolder()) {
            fileListing.setVisible(false);
        } else {
            if(shareByLinkAllowEditing.isChecked()) {
                boolean readOnly = (publicShare.getPermissions() & OCShare.READ_PERMISSION_FLAG) != 0;
                fileListing.setChecked(!readOnly);
            } else {
                fileListing.setVisible(false);
            }
        }
    }

    private void setupExpirationDateMenuItem(MenuItem expirationDate, Resources res) {
        long expirationDateValue = publicShare.getExpirationDate();
        if (expirationDateValue > 0) {
            String formattedDate =
                    SimpleDateFormat.getDateInstance().format(
                            new Date(expirationDateValue)
                    );
            expirationDate.setTitle(res.getString(
                    R.string.share_via_link_menu_expiration_date_label,
                    formattedDate
            ));
        } else {
            expirationDate.setTitle(res.getString(
                    R.string.share_via_link_menu_expiration_date_label,
                    res.getString(R.string.share_via_link_menu_expiration_date_never)
            ));
        }
    }

    private boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_link_hide_file_listing: {
                // TODO refresh share object after file listing has been set
                if (capabilities.getFilesFileDrop().isTrue()) {
                    ((FileActivity) getActivity()).getFileOperationsHelper().
                            setHideFileListingPermissionsToShare(publicShare, item.isChecked());
                } else {
                    // not supported in ownCloud
                    Snackbar.make(getView(), R.string.files_drop_not_supported, Snackbar.LENGTH_LONG)
                            .setAction(R.string.learn_more, v -> {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(getString(R.string.url_server_install)));
                                startActivity(i);
                            })
                            .show();
                }
                return true;
            }
            case R.id.action_share_link_password: {
                // TODO refresh share object after password has been set
                requestPasswordForShareViaLink(false);
                return true;
            }
            case R.id.action_share_link_expiration_date: {
                // TODO refresh share object after new exp. date has been set
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.newInstance(file, -1);
                dialog.show(
                        getActivity().getSupportFragmentManager(),
                        ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
                );
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void unshareWith(OCShare share) {
        ((FileActivity) getActivity()).getFileOperationsHelper()
                .unshareFileWithUserOrGroup(file, share.getShareType(), share.getShareWith());
    }

    @Override
    public int updatePermissionsToShare(OCShare share, boolean canReshare, boolean canEdit,
                                        boolean canEditCreate, boolean canEditChange,
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

        ((FileActivity) getActivity()).getFileOperationsHelper().
                setPermissionsToShare(
                        share,
                        permissions
                )
        ;

        return permissions;
    }

    /**
     * Starts a dialog that requests a password to the user to protect a share link.
     *
     * @param createShare When 'true', the request for password will be followed by the creation of a new
     *                    public link; when 'false', a public share is assumed to exist, and the password
     *                    is bound to it.
     */
    public void requestPasswordForShareViaLink(boolean createShare) {
        SharePasswordDialogFragment dialog = SharePasswordDialogFragment.newInstance(file, createShare);
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT);
    }

    /**
     * Get known server capabilities from DB
     * 
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshCapabilitiesFromDB() {
        if (((FileActivity) getActivity()).getStorageManager() != null) {
            capabilities = ((FileActivity) getActivity()).getStorageManager().getCapability(account.name);
        }
    }

    /**
     * Get public link from the DB to fill in the "Share link" section in the UI.
     * 
     * Takes into account server capabilities before reading database.
     * 
     * Depends on the parent Activity provides a {@link com.owncloud.android.datamodel.FileDataStorageManager}
     * instance ready to use. If not ready, does nothing.
     */
    public void refreshPublicShareFromDB() {
        if (isPublicShareDisabled()) {
            hidePublicShare();
        } else if (((FileActivity) getActivity()).getStorageManager() != null) {
            // Get public share
            publicShare = ((FileActivity) getActivity()).getStorageManager().getFirstShareByPathAndType(
                    file.getRemotePath(),
                    ShareType.PUBLIC_LINK,
                    ""
            );

            // Update public share section
            updatePublicShareSection();
        }
    }

    /**
     * Updates in the UI the section about public share with the information in the current public share bound to
     * mFile, if any.
     */
    private void updatePublicShareSection() {
        if (publicShare != null && ShareType.PUBLIC_LINK.equals(publicShare.getShareType())) {
            shareByLink.setChecked(true);

            if (publicShare.getPermissions() > OCShare.READ_PERMISSION_FLAG) {
                shareByLinkAllowEditing.setChecked(true);
            } else {
                shareByLinkAllowEditing.setChecked(false);
            }
        }
    }

    /**
     * @return 'True' when public share is disabled in the server.
     */
    private boolean isPublicShareDisabled() {
        return (capabilities != null && capabilities.getFilesSharingPublicEnabled().isFalse());
    }

    /**
     * Hides all the UI elements related to public share.
     */
    private void hidePublicShare() {
        shareByLinkContainer.setVisibility(View.GONE);
    }
}
