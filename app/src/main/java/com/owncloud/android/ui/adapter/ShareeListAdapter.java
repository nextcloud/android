/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2015-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareInternalShareLinkBinding;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.databinding.FileDetailsSharePublicLinkAddNewItemBinding;
import com.owncloud.android.databinding.FileDetailsShareSecureFileDropAddNewItemBinding;
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter to show a user/group/email/remote in Sharing list in file details view.
 */
public class ShareeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements DisplayUtils.AvatarGenerationListener {

    private final ShareeListAdapterListener listener;
    private final FileActivity fileActivity;
    private List<OCShare> shares;
    private final float avatarRadiusDimension;
    private final String userId;
    private final User user;
    private final ViewThemeUtils viewThemeUtils;
    private final boolean encrypted;

    public ShareeListAdapter(FileActivity fileActivity,
                             List<OCShare> shares,
                             ShareeListAdapterListener listener,
                             String userId,
                             User user,
                             final ViewThemeUtils viewThemeUtils,
                             boolean encrypted) {
        this.fileActivity = fileActivity;
        this.shares = shares;
        this.listener = listener;
        this.userId = userId;
        this.user = user;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;

        avatarRadiusDimension = fileActivity.getResources().getDimension(R.dimen.user_icon_radius);

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
            case PUBLIC_LINK, EMAIL -> {
                return new LinkShareViewHolder(
                    FileDetailsShareLinkShareItemBinding.inflate(LayoutInflater.from(fileActivity),
                                                                 parent,
                                                                 false),
                    fileActivity,
                    viewThemeUtils);
            }
            case NEW_PUBLIC_LINK -> {
                if (encrypted) {
                    return new NewSecureFileDropViewHolder(
                        FileDetailsShareSecureFileDropAddNewItemBinding.inflate(LayoutInflater.from(fileActivity),
                                                                                parent,
                                                                                false)
                    );
                } else {
                    return new NewLinkShareViewHolder(
                        FileDetailsSharePublicLinkAddNewItemBinding.inflate(LayoutInflater.from(fileActivity),
                                                                            parent,
                                                                            false)
                    );
                }
            }
            case INTERNAL -> {
                return new InternalShareViewHolder(
                    FileDetailsShareInternalShareLinkBinding.inflate(LayoutInflater.from(fileActivity), parent, false),
                    fileActivity);
            }
            default -> {
                return new ShareViewHolder(FileDetailsShareShareItemBinding.inflate(LayoutInflater.from(fileActivity),
                                                                                    parent,
                                                                                    false),
                                           user,
                                           fileActivity,
                                           viewThemeUtils);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (shares == null || shares.size() <= position) {
            return;
        }

        final OCShare share = shares.get(position);

        if (holder instanceof LinkShareViewHolder publicShareViewHolder) {
            publicShareViewHolder.bind(share, listener);
        } else if (holder instanceof InternalShareViewHolder internalShareViewHolder) {
            internalShareViewHolder.bind(share, listener);
        } else if (holder instanceof NewLinkShareViewHolder newLinkShareViewHolder) {
            newLinkShareViewHolder.bind(listener);
        } else if (holder instanceof NewSecureFileDropViewHolder newSecureFileDropViewHolder) {
            newSecureFileDropViewHolder.bind(listener);
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

    @SuppressLint("NotifyDataSetChanged")
    public void addShares(List<OCShare> sharesToAdd) {
        shares.addAll(sharesToAdd);
        sortShares();
        notifyDataSetChanged();
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        if (callContext instanceof ImageView iv) {
            iv.setImageDrawable(avatarDrawable);
        }
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        if (callContext instanceof ImageView iv) {
            // needs to be changed once federated users have avatars
            return String.valueOf(iv.getTag()).equals(tag.split("@")[0]);
        }
        return false;
    }

    @SuppressLint("NotifyDataSetChanged")
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

        links.sort((o1, o2) -> Long.compare(o2.getSharedDate(), o1.getSharedDate()));
        users.sort((o1, o2) -> Long.compare(o2.getSharedDate(), o1.getSharedDate()));

        shares = links;
        shares.addAll(users);

        // add internal share link at end
        if (!encrypted) {
            final OCShare ocShare = new OCShare();
            ocShare.setShareType(ShareType.INTERNAL);
            shares.add(ocShare);
        }
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
