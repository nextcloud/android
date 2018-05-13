/**
 *   ownCloud Android client application
 *
 *   @author masensio
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

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.Context;
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
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.ui.TextDrawable;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Adapter to show a user/group in Share With List in Details View
 */
public class UserListAdapter extends ArrayAdapter {


    private ShareeListAdapterListener listener;
    private OCCapability capabilities;
    private Context context;
    private ArrayList<OCShare> shares;
    private float avatarRadiusDimension;

    public UserListAdapter(Context context, int resource, ArrayList<OCShare> shares,
                           Account account, ShareeListAdapterListener listener) {
        super(context, resource);
        this.context = context;
        this.shares = shares;
        this.listener = listener;

        this.capabilities = new FileDataStorageManager(
                account,
                getContext().getContentResolver()
        ).getCapability(account.name);

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
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_group);
                }
            } else if (share.getShareType() == ShareType.EMAIL) {
                name = getContext().getString(R.string.share_email_clarification, name);
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_email);
                }
            } else {
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_user);
                }
            }
            userName.setText(name);

            /// bind listener to edit privileges
            editShareButton.setOnClickListener(v -> onOverflowIconClicked(v, shares.get(position)));
        }
        return view;
    }

    private void onOverflowIconClicked(View view, OCShare share) {
        PopupMenu popup = new PopupMenu(context, view);
        // TODO add folder edit detail permissions
        popup.inflate(R.menu.file_detail_sharing_menu);

        prepareOptionsMenu(popup.getMenu(), share);

        popup.setOnMenuItemClickListener(item -> optionsItemSelected(item, share));
        popup.show();
    }

    private void prepareOptionsMenu(Menu menu, OCShare share) {
        refresMenuForShare(share, menu);
    }

    private boolean optionsItemSelected(MenuItem item, OCShare share) {
        switch (item.getItemId()) {
            case R.id.action_can_edit: {
                // TODO calculate boolean flags
                listener.updatePermissionsToShare(share, true, true, true, true, true);
                return true;
            }
            case R.id.action_can_reshare: {
                // TODO calculate boolean flags
                listener.updatePermissionsToShare(share, true, true, true, true, true);
                return true;
            }
            // TODO add folder edit detail permissions
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

    /**
     * Updates the sharee's menu with the current permissions of the {@link OCShare}
     *
     * @param share the shared file
     * @param menu  the menu of the sharee/shared file
     */
    private void refresMenuForShare(OCShare share, Menu menu) {
        // TODO add folder edit detail permissions
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
    }

    public interface ShareeListAdapterListener {
        /**
         * unshare with given sharee.
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
         */
        void updatePermissionsToShare(OCShare share, boolean canReshare, boolean canEdit, boolean canEditCreate, boolean canEditChange, boolean canEditDelete);
    }
}
