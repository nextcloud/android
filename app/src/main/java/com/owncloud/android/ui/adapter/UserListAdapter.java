/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   @author Chris Narkiewicz <hello@ezaquarii.com>
 *   @author Nick Antoniou
 *
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2019 Nick Antoniou
 *   Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.databinding.AccountActionBinding;
import com.owncloud.android.databinding.AccountItemBinding;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * This Adapter populates a RecyclerView with all accounts within the app.
 */
public class UserListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
                                implements DisplayUtils.AvatarGenerationListener {
    private static final String TAG = UserListAdapter.class.getSimpleName();

    private float accountAvatarRadiusDimension;
    private final BaseActivity context;
    private List<UserListItem> values;
    private Listener accountListAdapterListener;
    private UserAccountManager accountManager;

    public static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";
    public static final int KEY_USER_INFO_REQUEST_CODE = 13;
    private ClickListener clickListener;
    private boolean showAddAccount;
    private boolean showDotsMenu;

    public UserListAdapter(BaseActivity context,
                           UserAccountManager accountManager,
                           List<UserListItem> values,
                           ClickListener clickListener,
                           boolean showAddAccount,
                           boolean showDotsMenu) {
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
            return new AccountViewHolderItem(AccountItemBinding.inflate(LayoutInflater.from(context), parent, false));
        } else {
            return new AddAccountViewHolderItem(
                AccountActionBinding.inflate(LayoutInflater.from(context), parent, false));
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
                item.bind(user, userListItem.isEnabled(), this);
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

        private AccountItemBinding binding;
        private User user;

        AccountViewHolderItem(@NonNull AccountItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            ThemeDrawableUtils.tintDrawable(binding.ticker.getDrawable(), ThemeColorUtils.primaryColor(context, true));

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

        public void bind(User user, boolean userListItemEnabled, DisplayUtils.AvatarGenerationListener avatarGenerationListener) {
            setData(user);
            setUser(user);
            setUsername(user);
            setAvatar(user, avatarGenerationListener);
            setCurrentlyActiveState(user);

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
                binding.userIcon.setImageResource(R.drawable.ic_user);
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
                binding.userName.setText(UserAccountManager.getUsername(user.toPlatformAccount()));
            }
            binding.userName.setTag(user.getAccountName());
        }
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AddAccountViewHolderItem extends RecyclerView.ViewHolder {

        AddAccountViewHolderItem(@NonNull AccountActionBinding binding) {
            super(binding.getRoot());
        }

        /**
         * Sets up a View to be used for adding a new account
         *
         * @param accountListAdapterListener {@link Listener}
         */
        private void bind(Listener accountListAdapterListener) {
            // bind action listener
            boolean isProviderOrOwnInstallationVisible = itemView.getContext().getResources()
                .getBoolean(R.bool.show_provider_or_own_installation);

            if (isProviderOrOwnInstallationVisible) {
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
