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
import android.widget.TextView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

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
    private Drawable tintedCheck;
    private UserAccountManager accountManager;

    public static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";
    public static final int KEY_USER_INFO_REQUEST_CODE = 13;
    private ClickListener clickListener;
    private boolean showAddAccount;

    public UserListAdapter(BaseActivity context,
                           UserAccountManager accountManager,
                           List<UserListItem> values,
                           Drawable tintedCheck,
                           ClickListener clickListener,
                           boolean showAddAccount) {
        this.context = context;
        this.accountManager = accountManager;
        this.values = values;
        if (context instanceof Listener) {
            this.accountListAdapterListener = (Listener) context;
        }
        this.accountAvatarRadiusDimension = context.getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
        this.tintedCheck = tintedCheck;
        this.clickListener = clickListener;
        this.showAddAccount = showAddAccount;
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
        View view;
        if (UserListItem.TYPE_ACCOUNT == viewType) {
            view = LayoutInflater.from(context).inflate(R.layout.account_item, parent, false);
            AccountViewHolderItem viewHolder = new AccountViewHolderItem(view);
            viewHolder.checkViewItem.setImageDrawable(tintedCheck);
            return viewHolder;
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.account_action, parent, false);
            return new AddAccountViewHolderItem(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UserListItem userListItem = values.get(position);

        if (userListItem != null) {
            // create account item
            if (UserListItem.TYPE_ACCOUNT == userListItem.getType()) {
                final User user = userListItem.getUser();
                AccountViewHolderItem item = (AccountViewHolderItem)holder;
                item.setData(user);
                setUser(item, user);
                setUsername(item, user);
                setAvatar(item, user);
                setCurrentlyActiveState(item, user);

                TextView usernameView = item.usernameViewItem;
                TextView accountView = item.accountViewItem;

                if (!userListItem.isEnabled()) {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    usernameView.setPaintFlags(usernameView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    accountView.setPaintFlags(accountView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }

            } // create add account action item
            else if (UserListItem.TYPE_ACTION_ADD == userListItem.getType() && accountListAdapterListener != null) {
                setupAddAccountListItem((AddAccountViewHolderItem)holder);
            }
        }
    }

    /**
     * Sets up a View to be used for adding a new account
     *
     * @param holder the add account view holder
     */
    private void setupAddAccountListItem(AddAccountViewHolderItem holder) {
        View actionView = holder.itemView;

        holder.usernameViewItem.setTextColor(ThemeUtils.primaryColor(context, true));

        // bind action listener
        boolean isProviderOrOwnInstallationVisible = context.getResources()
                .getBoolean(R.bool.show_provider_or_own_installation);

        if (isProviderOrOwnInstallationVisible) {
            actionView.setOnClickListener(v -> accountListAdapterListener.showFirstRunActivity());
        } else {
            actionView.setOnClickListener(v -> accountListAdapterListener.startAccountCreation());
        }
    }

    /**
     * Sets the name of the account, in the view holder
     *
     * @param viewHolder the view holder that contains the account
     * @param user the account
     */
    private void setUser(AccountViewHolderItem viewHolder, User user) {
        viewHolder.accountViewItem.setText(DisplayUtils.convertIdn(user.getAccountName(), false));
        viewHolder.accountViewItem.setTag(user.getAccountName());
    }

    /**
     * Sets the current active state of the account to true if it is the account being used currently,
     * false otherwise
     *
     * @param viewHolder the view holder that contains the account
     * @param user the account
     */
    private void setCurrentlyActiveState(AccountViewHolderItem viewHolder, User user) {
        User currentUser = accountManager.getUser();
        if (currentUser.nameEquals(user)) {
            viewHolder.checkViewItem.setVisibility(View.VISIBLE);
        } else {
            viewHolder.checkViewItem.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Sets the avatar of the account
     *
     * @param viewHolder the view holder that contains the account
     * @param user the account
     */
    private void setAvatar(AccountViewHolderItem viewHolder, User user) {
        try {
            View viewItem = viewHolder.imageViewItem;
            viewItem.setTag(user.getAccountName());
            DisplayUtils.setAvatar(user, this, accountAvatarRadiusDimension, context.getResources(), viewItem,
                                   context);
        } catch (Exception e) {
            Log_OC.e(TAG, "Error calculating RGB value for account list item.", e);
            // use user icon as a fallback
            viewHolder.imageViewItem.setImageResource(R.drawable.ic_user);
        }
    }

    /**
     * Sets the username of the account
     *
     * @param viewHolder the view holder that contains the account
     * @param user the account
     */
    private void setUsername(AccountViewHolderItem viewHolder, User user) {
        try {
            OwnCloudAccount oca = user.toOwnCloudAccount();
            viewHolder.usernameViewItem.setText(oca.getDisplayName());
        } catch (Exception e) {
            Log_OC.w(TAG, "Account not found right after being read; using account name instead");
            viewHolder.usernameViewItem.setText(UserAccountManager.getUsername(user.toPlatformAccount()));
        }
        viewHolder.usernameViewItem.setTag(user.getAccountName());
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
        private ImageView imageViewItem;
        private ImageView checkViewItem;

        private TextView usernameViewItem;
        private TextView accountViewItem;

        private User user;


        AccountViewHolderItem(@NonNull View view) {
            super(view);
            this.imageViewItem = view.findViewById(R.id.user_icon);
            this.checkViewItem = view.findViewById(R.id.ticker);
            this.usernameViewItem = view.findViewById(R.id.user_name);
            this.accountViewItem = view.findViewById(R.id.account);
            ImageView accountMenu = view.findViewById(R.id.account_menu);

            view.setOnClickListener(this);
            if(context instanceof ReceiveExternalFilesActivity) {
                accountMenu.setVisibility(View.GONE);
            }
            accountMenu.setOnClickListener(this);
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
    }

    /**
     * Account ViewHolderItem to get smooth scrolling.
     */
    static class AddAccountViewHolderItem extends RecyclerView.ViewHolder {
        private TextView usernameViewItem;
        AddAccountViewHolderItem(@NonNull View view) {
            super(view);
            this.usernameViewItem = view.findViewById(R.id.user_name);
        }
    }

    public interface ClickListener {
        void onOptionItemClicked(User user, View view);

        void onAccountClicked(User user);
    }
}
