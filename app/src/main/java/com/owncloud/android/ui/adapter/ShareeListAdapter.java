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
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsShareInternalShareLinkBinding;
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding;
import com.owncloud.android.databinding.FileDetailsSharePublicLinkAddNewItemBinding;
import com.owncloud.android.databinding.FileDetailsShareSecureFileDropAddNewItemBinding;
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding;
import com.owncloud.android.datamodel.SharesType;
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
    private final SharesType sharesType;
    private boolean showAll = false;

    public ShareeListAdapter(FileActivity fileActivity,
                             List<OCShare> shares,
                             ShareeListAdapterListener listener,
                             String userId,
                             User user,
                             final ViewThemeUtils viewThemeUtils,
                             boolean encrypted,
                             SharesType sharesType) {
        this.fileActivity = fileActivity;
        this.shares = shares;
        this.listener = listener;
        this.userId = userId;
        this.user = user;
        this.viewThemeUtils = viewThemeUtils;
        this.encrypted = encrypted;
        this.sharesType = sharesType;

        avatarRadiusDimension = fileActivity.getResources().getDimension(R.dimen.user_icon_radius);

        sortShares();
    }

    @Override
    public int getItemViewType(int position) {
        if (shares == null) {
            return 0;
        }

        if (position < 0 || position >= shares.size()) {
            return 0;
        }

        final var share = shares.get(position);
        if (share == null) {
            return 0;
        }

        final var shareType = share.getShareType();
        if (shareType == null) {
            return 0;
        }

        return shareType.getValue();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        boolean shareViaLink = MDMConfig.INSTANCE.shareViaLink(fileActivity);
        final var parentViewGroup = LayoutInflater.from(fileActivity);

        if (!shareViaLink) {
            final var binding = FileDetailsShareInternalShareLinkBinding.inflate(parentViewGroup, parent, false);
            return new InternalShareViewHolder(binding, fileActivity);
        }

        switch (ShareType.fromValue(viewType)) {
            case PUBLIC_LINK, EMAIL -> {
                final var binding = FileDetailsShareLinkShareItemBinding.inflate(parentViewGroup, parent, false);
                return new LinkShareViewHolder(binding, fileActivity, viewThemeUtils, encrypted);
            }
            case NEW_PUBLIC_LINK -> {
                if (encrypted) {
                    final var binding = FileDetailsShareSecureFileDropAddNewItemBinding.inflate(parentViewGroup, parent, false);
                    return new NewSecureFileDropViewHolder(binding);
                } else {
                    final var binding = FileDetailsSharePublicLinkAddNewItemBinding.inflate(parentViewGroup, parent, false);
                    return new NewLinkShareViewHolder(binding);
                }
            }
            case INTERNAL -> {
                final var binding = FileDetailsShareInternalShareLinkBinding.inflate(parentViewGroup, parent, false);
                return new InternalShareViewHolder(binding, fileActivity);
            }
            default -> {
                final var binding = FileDetailsShareShareItemBinding.inflate(parentViewGroup, parent, false);
                return new ShareViewHolder(binding, user, fileActivity, viewThemeUtils, encrypted);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (shares == null || shares.size() <= position) {
            return;
        }

        final OCShare share = shares.get(position);

        boolean shareViaLink = MDMConfig.INSTANCE.shareViaLink(fileActivity);

        if (!shareViaLink) {
            if (holder instanceof InternalShareViewHolder internalShareViewHolder) {
                internalShareViewHolder.bind(share, listener);
            }

            return;
        }


        if (holder instanceof LinkShareViewHolder publicShareViewHolder) {
            publicShareViewHolder.bind(share, listener, position);
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
        boolean shareViaLink = MDMConfig.INSTANCE.shareViaLink(fileActivity);
        if (shareViaLink) {
            if (showAll) {
                return shares.size();
            } else {
                return Math.min(shares.size(), 3);
            }
        } else {
            return 1;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void toggleShowAll() {
        showAll = !showAll;
        notifyDataSetChanged();
    }

    public boolean isShowAll() {
        return showAll;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addShares(List<OCShare> sharesToAdd) {
        shares.addAll(sharesToAdd);
        sortShares();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removeAll() {
        shares.clear();
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

    public void remove(OCShare share) {
        int position = shares.indexOf(share);
        if (position != -1) {
            shares.remove(position);
            notifyItemRemoved(position);
        }
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
        if (!encrypted && sharesType == SharesType.INTERNAL) {
            final OCShare ocShare = new OCShare();
            ocShare.setShareType(ShareType.INTERNAL);
            shares.add(ocShare);
        }
    }

    public List<OCShare> getShares() {
        return shares;
    }
}
