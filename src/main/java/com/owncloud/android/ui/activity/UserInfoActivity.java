/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.gson.Gson;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * This Activity presents the user information.
 */
public class UserInfoActivity extends FileActivity {
    public static final String KEY_ACCOUNT = "ACCOUNT";

    private static final String TAG = UserInfoActivity.class.getSimpleName();
    private static final String KEY_USER_DATA = "USER_DATA";
    private static final String KEY_DIRECT_REMOVE = "DIRECT_REMOVE";

    private static final int KEY_DELETE_CODE = 101;

    @BindView(R.id.empty_list_view) protected LinearLayout emptyContentContainer;
    @BindView(R.id.empty_list_view_text) protected TextView emptyContentMessage;
    @BindView(R.id.empty_list_view_headline) protected TextView emptyContentHeadline;
    @BindView(R.id.empty_list_icon) protected ImageView emptyContentIcon;
    @BindView(R.id.user_info_view) protected LinearLayout userInfoView;
    @BindView(R.id.user_icon) protected ImageView avatar;
    @BindView(R.id.userinfo_username) protected TextView userName;
    @BindView(R.id.userinfo_username_full) protected TextView fullName;
    @BindView(R.id.user_info_list) protected RecyclerView mUserInfoList;
    @BindView(R.id.empty_list_progress) protected ProgressBar multiListProgressBar;
    
    @BindString(R.string.user_information_retrieval_error) protected String sorryMessage;

    private float mCurrentAccountAvatarRadiusDimension;

    private Unbinder unbinder;

    private UserInfo userInfo;
    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        account = Parcels.unwrap(bundle.getParcelable(KEY_ACCOUNT));

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_USER_DATA)) {
            userInfo = Parcels.unwrap(savedInstanceState.getParcelable(KEY_USER_DATA));
        }

        mCurrentAccountAvatarRadiusDimension = getResources().getDimension(R.dimen.nav_drawer_header_avatar_radius);

        setContentView(R.layout.user_info_layout);
        unbinder = ButterKnife.bind(this);

        setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
        onAccountSet(false);

        boolean useBackgroundImage = URLUtil.isValidUrl(
                getStorageManager().getCapability(account.name).getServerBackground());

        setupToolbar(useBackgroundImage);
        updateActionBarTitleAndHomeButtonByString("");

        mUserInfoList.setAdapter(new UserInfoAdapter(null, ThemeUtils.primaryColor(getAccount(), this)));
        mUserInfoList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (userInfo != null) {
            populateUserInfoUi(userInfo);
        } else {
            setMultiListLoadingMessage();
            fetchAndSetData();
        }

        setHeaderImage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_info_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.delete_account:
                openAccountRemovalConfirmationDialog(account, getFragmentManager(), false);
                break;
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    private void setMultiListLoadingMessage() {
        if (emptyContentContainer != null) {
            emptyContentHeadline.setText(R.string.file_list_loading);
            emptyContentMessage.setText("");

            emptyContentIcon.setVisibility(View.GONE);
            emptyContentMessage.setVisibility(View.GONE);
            multiListProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryColor(this),
                    PorterDuff.Mode.SRC_IN);
            multiListProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void setErrorMessageForMultiList(String headline, String message, @DrawableRes int errorResource) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);
            emptyContentIcon.setImageResource(errorResource);

            multiListProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
            emptyContentMessage.setVisibility(View.VISIBLE);
        }
    }

    private void setHeaderImage() {
        if (getStorageManager().getCapability(account.name).getServerBackground() != null) {
            ViewGroup appBar = findViewById(R.id.appbar);

            if (appBar != null) {
                ImageView backgroundImageView = appBar.findViewById(R.id.drawer_header_background);

                String background = getStorageManager().getCapability(account.name).getServerBackground();
                int primaryColor = ThemeUtils.primaryColor(getAccount(), this);

                if (URLUtil.isValidUrl(background)) {
                    // background image
                    SimpleTarget target = new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                            Drawable[] drawables = {new ColorDrawable(primaryColor), resource};
                            LayerDrawable layerDrawable = new LayerDrawable(drawables);
                            backgroundImageView.setImageDrawable(layerDrawable);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            Drawable[] drawables = {new ColorDrawable(primaryColor),
                                    getResources().getDrawable(R.drawable.background)};
                            LayerDrawable layerDrawable = new LayerDrawable(drawables);
                            backgroundImageView.setImageDrawable(layerDrawable);
                        }
                    };

                    Glide.with(this)
                            .load(background)
                            .centerCrop()
                            .placeholder(R.drawable.background)
                            .error(R.drawable.background)
                            .crossFade()
                            .into(target);
                } else {
                    // plain color
                    backgroundImageView.setImageDrawable(new ColorDrawable(primaryColor));
                }
            }
        }
    }

    private void populateUserInfoUi(UserInfo userInfo) {
        userName.setText(account.name);
        avatar.setTag(account.name);
        DisplayUtils.setAvatar(account, UserInfoActivity.this, mCurrentAccountAvatarRadiusDimension, getResources(),
                getStorageManager(), avatar, this);

        int tint = ThemeUtils.primaryColor(account, this);

        if (!TextUtils.isEmpty(userInfo.getDisplayName())) {
            fullName.setText(userInfo.getDisplayName());
        }
        
        if (userInfo.getPhone() == null && userInfo.getEmail() == null && userInfo.getAddress() == null
                && userInfo.getTwitter() == null && userInfo.getWebsite() == null) {

            setErrorMessageForMultiList(getString(R.string.userinfo_no_info_headline),
                    getString(R.string.userinfo_no_info_text), R.drawable.ic_user);
        } else {
            emptyContentContainer.setVisibility(View.GONE);
            userInfoView.setVisibility(View.VISIBLE);

            if (mUserInfoList.getAdapter() instanceof UserInfoAdapter) {
                mUserInfoList.setAdapter(new UserInfoAdapter(createUserInfoDetails(userInfo), tint));
            }
        }
    }

    private List<UserInfoDetailsItem> createUserInfoDetails(UserInfo userInfo) {
        List<UserInfoDetailsItem> result = new LinkedList<>();

        addToListIfNeeded(result, R.drawable.ic_phone, userInfo.getPhone(), R.string.user_info_phone);
        addToListIfNeeded(result, R.drawable.ic_email, userInfo.getEmail(), R.string.user_info_email);
        addToListIfNeeded(result, R.drawable.ic_map_marker, userInfo.getAddress(), R.string.user_info_address);
        addToListIfNeeded(result, R.drawable.ic_web, DisplayUtils.beautifyURL(userInfo.getWebsite()),
                    R.string.user_info_website);
        addToListIfNeeded(result, R.drawable.ic_twitter, DisplayUtils.beautifyTwitterHandle(userInfo.getTwitter()),
                    R.string.user_info_twitter);

        return result;
    }

    private void addToListIfNeeded(List<UserInfoDetailsItem> info,
                                   @DrawableRes int icon,
                                   String text,
                                   @StringRes int contentDescriptionInt) {
        if (!TextUtils.isEmpty(text))
            info.add(new UserInfoDetailsItem(icon, text, getResources().getString(contentDescriptionInt)));
    }

    public static void openAccountRemovalConfirmationDialog(Account account, FragmentManager fragmentManager,
                                                            boolean removeDirectly) {
        UserInfoActivity.AccountRemovalConfirmationDialog dialog =
                UserInfoActivity.AccountRemovalConfirmationDialog.newInstance(account, removeDirectly);
        dialog.show(fragmentManager, "dialog");
    }

    public static class AccountRemovalConfirmationDialog extends DialogFragment {

        private Account account;

        public static UserInfoActivity.AccountRemovalConfirmationDialog newInstance(Account account,
                                                                                    boolean removeDirectly) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_ACCOUNT, account);
            bundle.putBoolean(KEY_DIRECT_REMOVE, removeDirectly);

            UserInfoActivity.AccountRemovalConfirmationDialog dialog = new
                    UserInfoActivity.AccountRemovalConfirmationDialog();
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            account = getArguments().getParcelable(KEY_ACCOUNT);
        }

        @Override
        public void onStart() {
            super.onStart();

            int color = ThemeUtils.primaryAccentColor(getActivity());

            AlertDialog alertDialog = (AlertDialog) getDialog();

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final boolean removeDirectly = getArguments().getBoolean(KEY_DIRECT_REMOVE);
            return new AlertDialog.Builder(getActivity(), R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.delete_account)
                    .setMessage(getResources().getString(R.string.delete_account_warning, account.name))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.common_ok,
                            (dialogInterface, i) -> {
                                // remove contact backup job
                                ContactsPreferenceActivity.cancelContactBackupJobForAccount(getActivity(), account);

                                ContentResolver contentResolver = getActivity().getContentResolver();

                                // disable daily backup
                                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(
                                        contentResolver);

                                arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
                                        "false");

                                String arbitraryDataPushString;

                                if (!TextUtils.isEmpty(arbitraryDataPushString = arbitraryDataProvider.getValue(
                                        account, PushUtils.KEY_PUSH)) &&
                                        !TextUtils.isEmpty(getResources().getString(R.string.push_server_url))) {
                                    Gson gson = new Gson();
                                    PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryDataPushString,
                                            PushConfigurationState.class);
                                    pushArbitraryData.setShouldBeDeleted(true);
                                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, PushUtils.KEY_PUSH,
                                            gson.toJson(pushArbitraryData));
                                    EventBus.getDefault().post(new TokenPushEvent());
                                }


                                if (getActivity() != null && !removeDirectly) {
                                    Bundle bundle = new Bundle();
                                    bundle.putParcelable(KEY_ACCOUNT, Parcels.wrap(account));
                                    Intent intent = new Intent();
                                    intent.putExtras(bundle);
                                    getActivity().setResult(KEY_DELETE_CODE, intent);
                                    getActivity().finish();
                                } else {
                                    AccountManager am = (AccountManager) getActivity()
                                            .getSystemService(ACCOUNT_SERVICE);

                                    am.removeAccount(account, null, null);

                                    Intent start = new Intent(getActivity(), FileDisplayActivity.class);
                                    start.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(start);
                                }

                            })
                    .setNegativeButton(R.string.common_cancel, null)
                    .create();
        }
    }

    private void fetchAndSetData() {
        Thread t = new Thread(() -> {
            RemoteOperation getRemoteUserInfoOperation = new GetRemoteUserInfoOperation();
            RemoteOperationResult result = getRemoteUserInfoOperation.execute(account, UserInfoActivity.this);

            if (result.isSuccess() && result.getData() != null) {
                userInfo = (UserInfo) result.getData().get(0);

                runOnUiThread(() -> populateUserInfoUi(userInfo));

            } else {
                // show error
                runOnUiThread(() -> setErrorMessageForMultiList(sorryMessage, result.getLogMessage(),
                        R.drawable.ic_list_empty_error));
                Log_OC.d(TAG, result.getLogMessage());
            }
        });

        t.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userInfo != null) {
            outState.putParcelable(KEY_USER_DATA, Parcels.wrap(userInfo));
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        PushUtils.pushRegistrationToServer();
    }


    protected class UserInfoDetailsItem {
        @DrawableRes public int icon;
        public String text;
        public String iconContentDescription;

        public UserInfoDetailsItem(@DrawableRes int icon, String text, String iconContentDescription) {
            this.icon = icon;
            this.text = text;
            this.iconContentDescription = iconContentDescription;
        }
    }

    protected class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
        protected List<UserInfoDetailsItem> mDisplayList;
        @ColorInt protected int mTintColor;

        public class ViewHolder extends RecyclerView.ViewHolder {

            @BindView(R.id.icon) protected ImageView icon = null;
            @BindView(R.id.text) protected TextView text = null;

            public ViewHolder(View itemView) {
                super(itemView);
                ButterKnife.bind(this, itemView);
            }
        }

        public UserInfoAdapter(List<UserInfoDetailsItem> displayList, @ColorInt int tintColor) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            mTintColor = tintColor;
        }

        public void setData(List<UserInfoDetailsItem> displayList) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.user_info_details_table_item, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UserInfoDetailsItem item = mDisplayList.get(position);
            holder.icon.setImageResource(item.icon);
            holder.text.setText(item.text);
            holder.icon.setContentDescription(item.iconContentDescription);
            DrawableCompat.setTint(holder.icon.getDrawable(), mTintColor);
        }

        @Override
        public int getItemCount() {
            return mDisplayList.size();
        }
    }
}
