/*
 * Nextcloud Android client application
 *
 * @author masensio
 * @author Andy Scherzinger
 * Copyright (C) 2015 ownCloud GmbH
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

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.NoteDialogFragment;
import com.owncloud.android.ui.fragment.util.SharingMenuHelper;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Adapter to show a user/group/email/remote in Sharing list in file details view.
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder>
        implements DisplayUtils.AvatarGenerationListener {

    private ShareeListAdapterListener listener;
    private OCCapability capabilities;
    private FragmentManager fragmentManager;
    private Context context;
    private int accentColor;
    private List<OCShare> shares;
    private float avatarRadiusDimension;
    private OCFile file;

    public UserListAdapter(FragmentManager fragmentManager, Context context, List<OCShare> shares, Account account,
                           OCFile file, ShareeListAdapterListener listener) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.shares = shares;
        this.listener = listener;
        this.file = file;

        accentColor = ThemeUtils.primaryAccentColor(context);
        capabilities = new FileDataStorageManager(account, context.getContentResolver()).getCapability(account.name);
        avatarRadiusDimension = context.getResources().getDimension(R.dimen.user_icon_radius);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_details_share_user_item, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (shares != null && shares.size() > position) {
            final OCShare share = shares.get(position);

            String name = share.getSharedWithDisplayName();

            switch (share.getShareType()) {
                case GROUP:
                    name = context.getString(R.string.share_group_clarification, name);
                    setImage(holder, name, R.drawable.ic_group);
                    break;
                case EMAIL:
                    name = context.getString(R.string.share_email_clarification, name);
                    setImage(holder, name, R.drawable.ic_email);
                    break;
                case ROOM:
                    name = context.getString(R.string.share_room_clarification, name);
                    setImage(holder, name, R.drawable.ic_chat_bubble);
                    break;
                default:
                    setImage(holder, name, R.drawable.ic_user);
                    break;
            }

            holder.name.setText(name);

            ThemeUtils.tintCheckbox(holder.allowEditing, accentColor);
            holder.allowEditing.setChecked(canEdit(share));
            holder.allowEditing.setOnClickListener(v -> allowEditClick(holder.allowEditing, share));

            // bind listener to edit privileges
            holder.editShareButton.setOnClickListener(v -> onOverflowIconClicked(v, holder.allowEditing, share));
        }
    }

    private void setImage(UserViewHolder holder, String name, @DrawableRes int fallback) {
        try {
            holder.avatar.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
        } catch (NoSuchAlgorithmException e) {
            holder.avatar.setImageResource(fallback);
        }
    }

    @Override
    public long getItemId(int position) {
        return shares.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    private void allowEditClick(AppCompatCheckBox checkBox, @NonNull OCShare share) {
        if (!share.isFolder()) {
            share.setPermissions(listener.updatePermissionsToShare(
                    share,
                    canReshare(share),
                    checkBox.isChecked(),
                    false,
                    false,
                    false
            ));
        } else {
            share.setPermissions(listener.updatePermissionsToShare(
                    share,
                    canReshare(share),
                    checkBox.isChecked(),
                    checkBox.isChecked(),
                    checkBox.isChecked(),
                    checkBox.isChecked()
            ));
        }
    }

    private void onOverflowIconClicked(View view, AppCompatCheckBox allowEditsCheckBox, OCShare share) {
        // use grey as fallback for elements where custom theming is not available
        if (ThemeUtils.themingEnabled(context)) {
            context.getTheme().applyStyle(R.style.FallbackThemingTheme, true);
        }
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.file_detail_sharing_menu);

        prepareOptionsMenu(popup.getMenu(), share);

        popup.setOnMenuItemClickListener(item -> optionsItemSelected(popup.getMenu(), item, allowEditsCheckBox, share));
        popup.show();
    }

    /**
     * Updates the sharee's menu with the current permissions of the {@link OCShare}
     *
     * @param menu  the menu of the sharee/shared file
     * @param share the shared file
     */
    private void prepareOptionsMenu(Menu menu, OCShare share) {

        MenuItem editCreateItem = menu.findItem(R.id.action_can_edit_create);
        MenuItem editChangeItem = menu.findItem(R.id.action_can_edit_change);
        MenuItem editDeleteItem = menu.findItem(R.id.action_can_edit_delete);

        MenuItem hideFileListingItem = menu.findItem(R.id.action_hide_file_listing);
        MenuItem passwordItem = menu.findItem(R.id.action_password);
        MenuItem expirationDateItem = menu.findItem(R.id.action_expiration_date);

        MenuItem reshareItem = menu.findItem(R.id.action_can_reshare);

        MenuItem sendNoteItem = menu.findItem(R.id.action_share_send_note);

        if (isReshareForbidden(share)) {
            reshareItem.setVisible(false);
        }
        reshareItem.setChecked(canReshare(share));

        if (share.getShareType() == ShareType.EMAIL) {
            SharingMenuHelper.setupHideFileListingMenuItem(
                    hideFileListingItem,
                    file.isFolder(),
                    canEdit(share),
                    share.getPermissions()
            );
            SharingMenuHelper.setupPasswordMenuItem(passwordItem, share.isPasswordProtected());

            reshareItem.setVisible(false);
            editCreateItem.setVisible(false);
            editChangeItem.setVisible(false);
            editDeleteItem.setVisible(false);
        } else {
            if (file.isFolder() && isEditOptionsAvailable(share)) {
                /// TODO change areEditOptionsAvailable in order to delete !isFederated
                editCreateItem.setChecked(canCreate(share));
                editChangeItem.setChecked(canUpdate(share));
                editDeleteItem.setChecked(canDelete(share));
            } else {
                editCreateItem.setVisible(false);
                editChangeItem.setVisible(false);
                editDeleteItem.setVisible(false);
            }

            hideFileListingItem.setVisible(false);
            passwordItem.setVisible(false);
            expirationDateItem.setVisible(false);
        }

        SharingMenuHelper.setupExpirationDateMenuItem(
                menu.findItem(R.id.action_expiration_date), share.getExpirationDate(), context.getResources());

        sendNoteItem.setVisible(capabilities.getVersion().isNoteOnShareSupported());
    }

    private boolean isEditOptionsAvailable(OCShare share) {
        return !ShareType.FEDERATED.equals(share.getShareType());
    }

    private boolean isReshareForbidden(OCShare share) {
        return ShareType.FEDERATED.equals(share.getShareType()) ||
                (capabilities != null && capabilities.getFilesSharingResharing().isFalse());
    }

    private boolean canEdit(OCShare share) {
        return (share.getPermissions() &
                (OCShare.CREATE_PERMISSION_FLAG | OCShare.UPDATE_PERMISSION_FLAG | OCShare.DELETE_PERMISSION_FLAG)) > 0;
    }

    private boolean canCreate(OCShare share) {
        return (share.getPermissions() & OCShare.CREATE_PERMISSION_FLAG) > 0;
    }

    private boolean canUpdate(OCShare share) {
        return (share.getPermissions() & OCShare.UPDATE_PERMISSION_FLAG) > 0;
    }

    private boolean canDelete(OCShare share) {
        return (share.getPermissions() & OCShare.DELETE_PERMISSION_FLAG) > 0;
    }

    private boolean canReshare(OCShare share) {
        return (share.getPermissions() & OCShare.SHARE_PERMISSION_FLAG) > 0;
    }

    private boolean optionsItemSelected(Menu menu, MenuItem item, AppCompatCheckBox allowEditsCheckBox, OCShare share) {
        switch (item.getItemId()) {
            case R.id.action_can_edit_create:
            case R.id.action_can_edit_change:
            case R.id.action_can_edit_delete: {
                item.setChecked(!item.isChecked());
                if (item.isChecked() && !allowEditsCheckBox.isChecked()) {
                    allowEditsCheckBox.setChecked(true);
                }
                share.setPermissions(
                        updatePermissionsToShare(
                                share,
                                menu.findItem(R.id.action_can_reshare).isChecked(),
                                allowEditsCheckBox.isChecked(),
                                menu.findItem(R.id.action_can_edit_create).isChecked(),
                                menu.findItem(R.id.action_can_edit_change).isChecked(),
                                menu.findItem(R.id.action_can_edit_delete).isChecked())
                );
                return true;
            }
            case R.id.action_can_reshare: {
                item.setChecked(!item.isChecked());
                share.setPermissions(
                        updatePermissionsToShare(
                                share,
                                menu.findItem(R.id.action_can_reshare).isChecked(),
                                allowEditsCheckBox.isChecked(),
                                menu.findItem(R.id.action_can_edit_create).isChecked(),
                                menu.findItem(R.id.action_can_edit_change).isChecked(),
                                menu.findItem(R.id.action_can_edit_delete).isChecked())
                );
                return true;
            }
            case R.id.action_unshare: {
                listener.unshareWith(share);
                shares.remove(share);
                notifyDataSetChanged();
                return true;
            }
            case R.id.action_hide_file_listing: {
                item.setChecked(!item.isChecked());
                if (capabilities.getFilesFileDrop().isTrue()) {
                    listener.setHideFileListingPermissionsToShare(share, item.isChecked());
                } else {
                    // not supported in ownCloud
                    listener.showNotSupportedByOcMessage();
                }
                return true;
            }
            case R.id.action_password: {
                listener.requestPasswordForShare(share);
                return true;
            }
            case R.id.action_expiration_date: {
                ExpirationDatePickerDialogFragment dialog = ExpirationDatePickerDialogFragment.newInstance(share, -1);
                dialog.show(fragmentManager, ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG);
                return true;
            }
            case R.id.action_share_send_note:
                NoteDialogFragment dialog = NoteDialogFragment.newInstance(share);
                dialog.show(fragmentManager, NoteDialogFragment.NOTE_FRAGMENT);
                return true;
            default:
                return true;
        }
    }

    private int updatePermissionsToShare(OCShare share, boolean canReshare, boolean canEdit, boolean canEditCreate,
                                         boolean canEditChange, boolean canEditDelete) {
        return listener.updatePermissionsToShare(
                share,
                canReshare,
                canEdit,
                canEditCreate,
                canEditChange,
                canEditDelete
        );
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        if (callContext instanceof ImageView) {
            ImageView iv = (ImageView) callContext;
            iv.setImageDrawable(avatarDrawable);
        }
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        if (callContext instanceof ImageView) {
            ImageView iv = (ImageView) callContext;
            return String.valueOf(iv.getTag()).equals(tag);
        }
        return false;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar)
        ImageView avatar;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.allowEditing)
        AppCompatCheckBox allowEditing;
        @BindView(R.id.editShareButton)
        ImageView editShareButton;

        UserViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface ShareeListAdapterListener {
        /**
         * unshare with given sharee {@link OCShare}.
         *
         * @param share the share
         */
        void unshareWith(OCShare share);

        /**
         * Updates the permissions of the {@link OCShare}.
         *
         * @param share         the share to be updated
         * @param canReshare    reshare permission
         * @param canEdit       edit permission
         * @param canEditCreate create permission (folders only)
         * @param canEditChange change permission (folders only)
         * @param canEditDelete delete permission (folders only)
         * @return permissions value set
         */
        int updatePermissionsToShare(OCShare share,
                                     boolean canReshare,
                                     boolean canEdit,
                                     boolean canEditCreate,
                                     boolean canEditChange,
                                     boolean canEditDelete);

        void updateNoteToShare(OCShare share, String note);

        /**
         * show a snackbar that this feature is not supported by ownCloud.
         */
        void showNotSupportedByOcMessage();

        /**
         * Starts a dialog that requests a password to the user to protect a share.
         *
         * @param share the share for which a password shall be configured/removed
         */
        void requestPasswordForShare(OCShare share);

        /**
         * Updates a public share on a folder to set its hide file listing permission.
         * Starts a request to do it in {@link OperationsService}
         *
         * @param share           {@link OCShare} instance which permissions will be updated.
         * @param hideFileListing New state of the permission for editing the folder shared via link.
         */
        void setHideFileListingPermissionsToShare(OCShare share, boolean hideFileListing);

        void setHideFileDownloadPermissionToShare(OCFile file, boolean hideFileDownload);
    }
}
