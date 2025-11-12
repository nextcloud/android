/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Kilian Périsset <kilian.perisset@infomaniak.com>
 * SPDX-FileCopyrightText: 2020 Stefan Niedermann <info@niedermann.it>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Nick Antoniou <nikolasea@windowslive.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.R;
import com.owncloud.android.databinding.AccountActionBinding;
import com.owncloud.android.databinding.AccountItemBinding;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
                                implements DisplayUtils.AvatarGenerationListener {
    private static final String TAG = UserListAdapter.class.getSimpleName();

    private final float accountAvatarRadiusDimension;
    private final Context context;
    private List<UserListItem> values;
    private Listener accountListAdapterListener;
    private final UserAccountManager accountManager;

    public static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";
    public static final int KEY_USER_INFO_REQUEST_CODE = 13;
    private final ClickListener clickListener;
    private final boolean showAddAccount;
    private final boolean showDotsMenu;
    private boolean highlightCurrentlyActiveAccount;
    private final ViewThemeUtils viewThemeUtils;

    public UserListAdapter(Context context,
                           UserAccountManager accountManager,
                           List<UserListItem> values,
                           ClickListener clickListener,
                           boolean showAddAccount,
                           boolean showDotsMenu,
                           boolean highlightCurrentlyActiveAccount,
                           final ViewThemeUtils viewThemeUtils) {
        this.context = context;
        this.accountManager = accountManager;
        this.values = values;
        if (context instanceof Listener) {
            this.accountListAdapterListener = (Listener) context;
        }
        this.accountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.clickListener = clickListener;
        this.showAddAccount = showAddAccount;
        this.showDotsMenu = showDotsMenu;
        this.viewThemeUtils = viewThemeUtils;
        this.highlightCurrentlyActiveAccount = highlightCurrentlyActiveAccount;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == values.size() - 1 && showAddAccount) {
            return UserListItem.TYPE_ACTION_ADD;
        }
        return UserListItem.TYPE_ACCOUNT;
    }

    @Override
    public @NonNull
    RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (UserListItem.TYPE_ACCOUNT == viewType) {
            return new AccountViewHolderItem(AccountItemBinding.inflate(LayoutInflater.from(context),
                                                                        parent,
                                                                        false),
                                             viewThemeUtils);
        } else {
            return new AddAccountViewHolderItem(
                AccountActionBinding.inflate(LayoutInflater.from(context), parent, false),
                context
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UserListItem userListItem = values.get(position);

        if (userListItem != null) {
            // create account item
            if (UserListItem.TYPE_ACCOUNT == userListItem.getType()) {
                final User user = userListItem.getUser();
                AccountViewHolderItem item = (AccountViewHolderItem) holder;
                item.bind(user, userListItem.isEnabled(), highlightCurrentlyActiveAccount, this);
            } // create add account action item
            else if (UserListItem.TYPE_ACTION_ADD == userListItem.getType() && accountListAdapterListener != null) {
                ((AddAccountViewHolderItem) holder).bind(accountListAdapterListener);
            }
        }
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ImageView)callContext).setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return String.valueOf(((ImageView)callContext).getTag()).equals(tag);
    }

    /**
     * Returns the total number of items in the data set held by the adapter
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return this.values.size();
    }

    /**
     * Returns an UserListItem from the specified position in the values list
     *
     * @param position of the object to be returned
     * @return An UserListItem of the specified position
     */
    public UserListItem getItem(int position) {
        return values.get(position);
    }

    /**
     * Deletes the elements in the values list and notifies the Adapter
     */
    public void clear() {
        final int size = values.size();
        values.clear();
        notifyItemRangeRemoved(0, size);
    }

    /**
     * Adds all of the items to the data set
     *
     * @param items The item list to be added
     */
    @SuppressLint("NotifyDataSetChanged")
    public void addAll(List<UserListItem> items){
        if(values == null){
            values = new ArrayList<>();
        }
        values.addAll(items);
        notifyDataSetChanged();
    }

    public interface Listener {
        void showFirstRunActivity();
        void startAccountCreation();
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    class AccountViewHolderItem extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AccountItemBinding binding;
        private User user;

        AccountViewHolderItem(@NonNull AccountItemBinding binding, final ViewThemeUtils viewThemeUtils) {
            super(binding.getRoot());
            this.binding = binding;

            viewThemeUtils.platform.tintPrimaryDrawable(context, binding.ticker.getDrawable());

            binding.getRoot().setOnClickListener(this);
            if (showDotsMenu) {
                binding.accountMenu.setVisibility(View.VISIBLE);
                binding.accountMenu.setOnClickListener(this);
            } else {
                binding.accountMenu.setVisibility(View.GONE);
            }
        }

        public void setData(User user) {
            this.user = user;
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null && view.isEnabled()) {
                if (view.getId() == R.id.account_menu) {
                    clickListener.onOptionItemClicked(user, view);
                } else {
                    clickListener.onAccountClicked(user);
                }
            }
        }

        public void bind(User user,
                         boolean userListItemEnabled,
                         boolean highlightCurrentlyActiveAccount,
                         DisplayUtils.AvatarGenerationListener avatarGenerationListener) {
            setData(user);
            setUser(user);
            setUsername(user);
            setAvatar(user, avatarGenerationListener);
            if (highlightCurrentlyActiveAccount) {
                setCurrentlyActiveState(user);
            } else {
                binding.ticker.setVisibility(View.INVISIBLE);
            }

            if (!userListItemEnabled) {
                binding.userName.setPaintFlags(binding.userName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.account.setPaintFlags(binding.account.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                binding.userName.setPaintFlags(binding.userName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.account.setPaintFlags(binding.account.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

        }

        /**
         * Sets the name of the account, in the view holder
         *
         * @param user the account
         */
        private void setUser(User user) {
            binding.account.setText(DisplayUtils.convertIdn(user.getAccountName(), false));
            binding.account.setTag(user.getAccountName());
        }

        /**
         * Sets the current active state of the account to true if it is the account being used currently,
         * false otherwise
         *
         * @param user the account
         */
        private void setCurrentlyActiveState(User user) {
            User currentUser = accountManager.getUser();
            if (currentUser.nameEquals(user)) {
                binding.ticker.setVisibility(View.VISIBLE);
            } else {
                binding.ticker.setVisibility(View.INVISIBLE);
            }
        }

        /**
         * Sets the avatar of the account
         *
         * @param user the account
         */
        private void setAvatar(User user, DisplayUtils.AvatarGenerationListener avatarGenerationListener) {
            try {
                View viewItem = binding.userIcon;
                viewItem.setTag(user.getAccountName());
                DisplayUtils.setAvatar(user,
                                       avatarGenerationListener,
                                       accountAvatarRadiusDimension,
                                       context.getResources(),
                                       viewItem,
                                       context);
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
                // use user icon as a fallback
                binding.userIcon.setImageResource(R.drawable.ic_user_outline);
            }
        }

        /**
         * Sets the username of the account
         *
         * @param user the account
         */
        private void setUsername(User user) {
            try {
                OwnCloudAccount oca = user.toOwnCloudAccount();
                binding.userName.setText(oca.getDisplayName());
            } catch (Exception e) {
                Log_OC.w(TAG, "Account not found right after being read; using account name instead");
                binding.userName.setText(UserAccountManager.getUsername(user));
            }
            binding.userName.setTag(user.getAccountName());
        }
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AddAccountViewHolderItem extends RecyclerView.ViewHolder {

        private final Context context;

        AddAccountViewHolderItem(@NonNull AccountActionBinding binding, Context context) {
            super(binding.getRoot());
            this.context = context;
        }

        /**
         * Sets up a View to be used for adding a new account
         *
         * @param accountListAdapterListener {@link Listener}
         */
        private void bind(Listener accountListAdapterListener) {
            if (context == null) {
                Log_OC.d(TAG,"Context cannot be null, AddAccountViewHolderItem onClick is disabled");
                return;
            }

            if (MDMConfig.INSTANCE.showIntro(context)) {
                itemView.setOnClickListener(v -> accountListAdapterListener.showFirstRunActivity());
            } else {
                itemView.setOnClickListener(v -> accountListAdapterListener.startAccountCreation());
            }
        }
    }

    public interface ClickListener {
        void onOptionItemClicked(User user, View view);

        void onAccountClicked(User user);
    }
}
