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

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;

/**
 * Adapter to show a user/group/email/remote in Sharing list in file details view.
 */
public class UserListAdapter extends ArrayAdapter implements DisplayUtils.AvatarGenerationListener {

    private ShareeListAdapterListener listener;
    private OCCapability capabilities;
    private Context context;
    private ArrayList<OCShare> shares;
    private float avatarRadiusDimension;
    private Account account;
    private OCFile file;
    private FileDataStorageManager storageManager;

    public UserListAdapter(Context context, int resource, ArrayList<OCShare> shares,
                           Account account, OCFile file, ShareeListAdapterListener listener) {
        super(context, resource);
        this.context = context;
        this.shares = shares;
        this.listener = listener;
        this.account = account;
        this.file = file;

        storageManager = new FileDataStorageManager(account, getContext().getContentResolver());
        capabilities = storageManager.getCapability(account.name);

        avatarRadiusDimension = context.getResources().getDimension(R.dimen.standard_padding);
    }

    @Override
    public int getCount() {
        return shares.size();
    }

    @Override
    public Object getItem(int position) {
        return shares.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public @NonNull
    View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.file_details_share_user_item, parent, false);
        }

        if (shares != null && shares.size() > position) {
            OCShare share = shares.get(position);

            TextView userName = view.findViewById(R.id.userOrGroupName);
            final ImageView editShareButton = view.findViewById(R.id.editShareButton);
            ImageView icon = view.findViewById(R.id.userIcon);
            String name = share.getSharedWithDisplayName();
            if (share.getShareType() == ShareType.GROUP) {
                name = getContext().getString(R.string.share_group_clarification, name);
                icon.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
            } else if (share.getShareType() == ShareType.EMAIL) {
                name = getContext().getString(R.string.share_email_clarification, name);
                icon.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
            } else {
                icon.setTag(share.getShareWith());
                DisplayUtils.setAvatar(account, share.getShareWith(), this, avatarRadiusDimension,
                        context.getResources(), storageManager, icon, context);
            }
            userName.setText(name);

            /// bind listener to edit privileges
            editShareButton.setOnClickListener(v -> onOverflowIconClicked(v, shares.get(position)));
        }
        return view;
    }

    private void onOverflowIconClicked(View view, OCShare share) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.file_detail_sharing_menu);

        prepareOptionsMenu(popup.getMenu(), share);

        popup.setOnMenuItemClickListener(item -> optionsItemSelected(popup.getMenu(), item, share));
        popup.show();
    }

    /**
     * Updates the sharee's menu with the current permissions of the {@link OCShare}
     *
     * @param menu  the menu of the sharee/shared file
     * @param share the shared file
     */
    private void prepareOptionsMenu(Menu menu, OCShare share) {
        int sharePermissions = share.getPermissions();
        boolean isFederated = ShareType.FEDERATED.equals(share.getShareType());

        MenuItem reshareItem = menu.findItem(R.id.action_can_reshare);
        if (isFederated ||
                (capabilities != null && capabilities.getFilesSharingResharing().isFalse())) {
            reshareItem.setVisible(false);
        }
        reshareItem.setChecked((sharePermissions & OCShare.SHARE_PERMISSION_FLAG) > 0);

        MenuItem editItem = menu.findItem(R.id.action_can_edit);
        int anyUpdatePermission = OCShare.CREATE_PERMISSION_FLAG | OCShare.UPDATE_PERMISSION_FLAG |
                OCShare.DELETE_PERMISSION_FLAG;
        boolean canEdit = (sharePermissions & anyUpdatePermission) > 0;
        editItem.setChecked(canEdit);

        OwnCloudVersion serverVersion = AccountUtils.getServerVersion(account);
        boolean isNotReshareableFederatedSupported = serverVersion.isNotReshareableFederatedSupported();
        boolean areEditOptionsAvailable = !isFederated || isNotReshareableFederatedSupported;
        MenuItem editCreateItem = menu.findItem(R.id.action_can_edit_create);
        MenuItem editChangeItem = menu.findItem(R.id.action_can_edit_change);
        MenuItem editDeleteItem = menu.findItem(R.id.action_can_edit_delete);
        if (file.isFolder() && areEditOptionsAvailable) {
            /// TODO change areEditOptionsAvailable in order to delete !isFederated
            editCreateItem.setChecked((sharePermissions & OCShare.CREATE_PERMISSION_FLAG) > 0);
            editChangeItem.setChecked((sharePermissions & OCShare.UPDATE_PERMISSION_FLAG) > 0);
            editDeleteItem.setChecked((sharePermissions & OCShare.DELETE_PERMISSION_FLAG) > 0);
        } else {
            editCreateItem.setVisible(false);
            editChangeItem.setVisible(false);
            editDeleteItem.setVisible(false);
        }
    }

    private boolean optionsItemSelected(Menu menu, MenuItem item, OCShare share) {
        switch (item.getItemId()) {
            case R.id.action_can_edit: {
                item.setChecked(!item.isChecked());
                if (file.isFolder() && !item.isChecked()) {
                    menu.findItem(R.id.action_can_edit_create).setChecked(false);
                    menu.findItem(R.id.action_can_edit_change).setChecked(false);
                    menu.findItem(R.id.action_can_edit_delete).setChecked(false);
                }
                share.setPermissions(updatePermissionsToShare(share, menu));
                return true;
            }
            case R.id.action_can_edit_create:
            case R.id.action_can_edit_change:
            case R.id.action_can_edit_delete: {
                item.setChecked(!item.isChecked());
                if (item.isChecked() && !menu.findItem(R.id.action_can_edit).isChecked()) {
                    menu.findItem(R.id.action_can_edit).setChecked(true);
                }
                share.setPermissions(updatePermissionsToShare(share, menu));
                return true;
            }
            case R.id.action_can_reshare: {
                item.setChecked(!item.isChecked());
                share.setPermissions(updatePermissionsToShare(share, menu));
                return true;
            }
            case R.id.action_unshare: {
                listener.unshareWith(share);
                shares.remove(share);
                notifyDataSetChanged();
                return true;
            }
            default:
                return true;
        }
    }

    private int updatePermissionsToShare(OCShare share, Menu menu) {
        return listener.updatePermissionsToShare(
                share,
                menu.findItem(R.id.action_can_reshare).isChecked(),
                menu.findItem(R.id.action_can_edit).isChecked(),
                menu.findItem(R.id.action_can_edit_create).isChecked(),
                menu.findItem(R.id.action_can_edit_change).isChecked(),
                menu.findItem(R.id.action_can_edit_delete).isChecked()
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
    }
}
