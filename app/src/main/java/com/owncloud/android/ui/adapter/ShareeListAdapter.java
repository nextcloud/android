/*
 * Nextcloud Android client application
 *
 * @author masensio
 * @author Andy Scherzinger
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 * Copyright (C) 2015 ownCloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Nextcloud GmbH
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareInternalShareLinkBinding;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.databinding.FileDetailsSharePublicLinkAddNewItemBinding;
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.DisplayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter to show a user/group/email/remote in Sharing list in file details view.
 */
public class ShareeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements DisplayUtils.AvatarGenerationListener {

    private ShareeListAdapterListener listener;
    private Context context;
    private List<OCShare> shares;
    private float avatarRadiusDimension;
    private String userId;
    private User user;

    public ShareeListAdapter(Context context,
                             List<OCShare> shares,
                             ShareeListAdapterListener listener,
                             String userId,
                             User user) {
        this.context = context;
        this.shares = shares;
        this.listener = listener;
        this.userId = userId;
        this.user = user;

        avatarRadiusDimension = context.getResources().getDimension(R.dimen.user_icon_radius);

        sortShares();
    }

    @Override
    public int getItemViewType(int position) {
        return shares.get(position).getShareType().getValue();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (ShareType.fromValue(viewType)) {
            case PUBLIC_LINK:
            case EMAIL:
                return new LinkShareViewHolder(
                    FileDetailsShareLinkShareItemBinding.inflate(LayoutInflater.from(context),
                                                                 parent,
                                                                 false),
                    context);
            case NEW_PUBLIC_LINK:
                return new NewLinkShareViewHolder(
                    FileDetailsSharePublicLinkAddNewItemBinding.inflate(LayoutInflater.from(context),
                                                                        parent,
                                                                        false)
                );
            case INTERNAL:
                return new InternalShareViewHolder(
                    FileDetailsShareInternalShareLinkBinding.inflate(LayoutInflater.from(context), parent, false),
                    context);
            default:
                return new ShareViewHolder(FileDetailsShareShareItemBinding.inflate(LayoutInflater.from(context),
                                                                                    parent,
                                                                                    false),
                                           user,
                                           context);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (shares == null || shares.size() <= position) {
            return;
        }

        final OCShare share = shares.get(position);

        if (holder instanceof LinkShareViewHolder) {
            LinkShareViewHolder publicShareViewHolder = (LinkShareViewHolder) holder;
            publicShareViewHolder.bind(share, listener);
        } else if (holder instanceof InternalShareViewHolder) {
            InternalShareViewHolder internalShareViewHolder = (InternalShareViewHolder) holder;
            internalShareViewHolder.bind(share, listener);
        } else if (holder instanceof NewLinkShareViewHolder) {
            NewLinkShareViewHolder newLinkShareViewHolder = (NewLinkShareViewHolder) holder;
            newLinkShareViewHolder.bind(listener);
        } else {
            ShareViewHolder userViewHolder = (ShareViewHolder) holder;
            userViewHolder.bind(share, listener, this, userId, avatarRadiusDimension);
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

    public void addShares(List<OCShare> sharesToAdd) {
        shares.addAll(sharesToAdd);
        sortShares();
        notifyDataSetChanged();
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
            // needs to be changed once federated users have avatars
            return String.valueOf(iv.getTag()).equals(tag.split("@")[0]);
        }
        return false;
    }

    public void remove(OCShare share) {
        shares.remove(share);
        notifyDataSetChanged();
    }

    /**
     * sort all by creation time, then email/link shares on top
     */
    protected final void sortShares() {
        List<OCShare> links = new ArrayList<>();
        List<OCShare> users = new ArrayList<>();

        for (OCShare share : shares) {
            if (ShareType.PUBLIC_LINK == share.getShareType() || ShareType.EMAIL == share.getShareType()) {
                links.add(share);
            } else if (share.getShareType() != ShareType.INTERNAL) {
                users.add(share);
            }
        }

        Collections.sort(links, (o1, o2) -> Long.compare(o2.getSharedDate(), o1.getSharedDate()));
        Collections.sort(users, (o1, o2) -> Long.compare(o2.getSharedDate(), o1.getSharedDate()));

        shares = links;
        shares.addAll(users);

        // add internal share link at end
        shares.add(new OCShare().setShareType(ShareType.INTERNAL));
    }

    public List<OCShare> getShares() {
        return shares;
    }

    public void removeNewPublicShare() {
        for (OCShare share : shares) {
            if (share.getShareType() == ShareType.NEW_PUBLIC_LINK) {
                shares.remove(share);
                break;
            }
        }
    }
}
