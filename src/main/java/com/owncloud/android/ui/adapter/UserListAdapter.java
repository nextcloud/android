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
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.TextDrawable;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Adapter to show a user/group in Share With List in Details View
 */
public class UserListAdapter extends ArrayAdapter {

    private Context mContext;
    private ArrayList<OCShare> mShares;
    private float mAvatarRadiusDimension;

    public UserListAdapter(Context context, int resource, ArrayList<OCShare> shares) {
        super(context, resource);
        mContext = context;
        mShares = shares;

        mAvatarRadiusDimension = context.getResources().getDimension(R.dimen.standard_padding);
    }

    @Override
    public int getCount() {
        return mShares.size();
    }

    @Override
    public Object getItem(int position) {
        return mShares.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public @NonNull View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.file_details_share_user_item, parent, false);
        }

        if (mShares != null && mShares.size() > position) {
            OCShare share = mShares.get(position);

            TextView userName = view.findViewById(R.id.userOrGroupName);
            final ImageView editShareButton = view.findViewById(R.id.editShareButton);
            ImageView icon = view.findViewById(R.id.userIcon);
            String name = share.getSharedWithDisplayName();
            if (share.getShareType() == ShareType.GROUP) {
                name = getContext().getString(R.string.share_group_clarification, name);
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, mAvatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_group);
                }
            } else if (share.getShareType() == ShareType.EMAIL) {
                name = getContext().getString(R.string.share_email_clarification, name);
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, mAvatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_email);
                }
            } else {
                try {
                    icon.setImageDrawable(TextDrawable.createNamedAvatar(name, mAvatarRadiusDimension));
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                    icon.setImageResource(R.drawable.ic_user);
                }
            }
            userName.setText(name);

            /// bind listener to edit privileges
            editShareButton.setOnClickListener(v -> onOverflowIconClicked(v,mShares.get(position)));
        }
        return view;
    }

    private void onOverflowIconClicked(View view, OCShare share) {
        PopupMenu popup = new PopupMenu(mContext, view);
        popup.inflate(R.menu.file_detail_sharing_menu);

        prepareOptionsMenu(popup.getMenu(), share);

        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    private void prepareOptionsMenu(Menu menu, OCShare share) {
        // TODO implement menu filtering based on OCShare type
    }

    private boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_can_edit: {
                // TODO implement de/-selecting can edit
                return true;
            }
            case R.id.action_can_reshare: {
                // TODO implement de/-selecting can share
                return true;
            }
            case R.id.action_unshare: {
                // TODO implement unshare
                return true;
            }
            default:
                return true;
        }
    }
}