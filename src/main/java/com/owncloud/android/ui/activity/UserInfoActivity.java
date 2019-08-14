/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.UserInfo;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.repository.UserInfoRepository;
import com.owncloud.android.ui.adapter.UserInfoAdapter;
import com.owncloud.android.ui.components.UserInfoDetailsItem;
import com.owncloud.android.ui.viewModel.UserInfoViewModel;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.parceler.Parcels;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * This Activity presents the user information.
 */
public class UserInfoActivity extends FileActivity implements Injectable {
    public static final String KEY_ACCOUNT = "ACCOUNT";
    public static final String KEY_DIRECT_REMOVE = "DIRECT_REMOVE";

    private static final String TAG = UserInfoActivity.class.getSimpleName();

    public static final int KEY_DELETE_CODE = 101;

    @BindView(R.id.empty_list_view)
    protected LinearLayout emptyContentContainer;
    @BindView(R.id.empty_list_view_text)
    protected TextView emptyContentMessage;
    @BindView(R.id.empty_list_view_headline)
    protected TextView emptyContentHeadline;
    @BindView(R.id.empty_list_icon)
    protected ImageView emptyContentIcon;
    @BindView(R.id.user_info_view)
    protected LinearLayout userInfoView;
    @BindView(R.id.user_icon)
    protected ImageView avatar;
    @BindView(R.id.userinfo_username)
    protected TextView userName;
    @BindView(R.id.userinfo_username_full)
    protected TextView fullName;
    @BindView(R.id.user_info_list)
    protected RecyclerView mUserInfoList;
    @BindView(R.id.empty_list_progress)
    protected ProgressBar multiListProgressBar;
    @BindView(R.id.userinfo_quota)
    protected LinearLayout quotaView;
    @BindView(R.id.userinfo_quota_progressBar)
    protected ProgressBar quotaProgressBar;
    @BindView(R.id.userinfo_quota_percentage)
    protected TextView quotaPercentage;
    @BindView(R.id.quota_icon)
    protected ImageView quotaIcon;

    @BindString(R.string.user_information_retrieval_error)
    protected String sorryMessage;

    @Inject AppPreferences preferences;
    private float mCurrentAccountAvatarRadiusDimension;

    private Unbinder unbinder;

    private Account account;
    private UserInfoAdapter adapter;
    private @ColorRes int primaryColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();

        if (bundle == null) {
            throw new NullPointerException("Bundle may not be null");
        }

        account = Parcels.unwrap(bundle.getParcelable(KEY_ACCOUNT));

        primaryColor = ThemeUtils.primaryColor(account, true, this);

        UserInfoRepository userInfoRepository = new UserInfoRepository(Executors.newCachedThreadPool(),
                                                                       new FileDataStorageManager(account,
                                                                                                  getContentResolver()));

        UserInfoViewModel viewModel = ViewModelProviders.of(this).get(UserInfoViewModel.class);
        viewModel.init(account, userInfoRepository, getUserAccountManager());

        mCurrentAccountAvatarRadiusDimension = getResources().getDimension(R.dimen.nav_drawer_header_avatar_radius);

        setContentView(R.layout.user_info_layout);
        unbinder = ButterKnife.bind(this);

        setAccount(getUserAccountManager().getCurrentAccount());
        onAccountSet(false);

        boolean useBackgroundImage = URLUtil.isValidUrl(
            getStorageManager().getCapability(account.name).getServerBackground());

        setupToolbar(useBackgroundImage);
        updateActionBarTitleAndHomeButtonByString("");

        adapter = new UserInfoAdapter(null);
        mUserInfoList.setAdapter(adapter);

        viewModel.getUserInfo().observe(this, this::populateUserInfoUi);

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
                UserInfoViewModel.openAccountRemovalConfirmationDialog(account, getSupportFragmentManager(), false);
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

    private void setErrorMessageForMultiList(String headline, String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);
            emptyContentIcon.setImageResource(R.drawable.ic_user);

            multiListProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
            emptyContentMessage.setVisibility(View.VISIBLE);
        }
    }

    // todo move to viewModel
    private void setHeaderImage() {
        if (getStorageManager().getCapability(account.name).getServerBackground() != null) {
            ViewGroup appBar = findViewById(R.id.appbar);

            if (appBar != null) {
                ImageView backgroundImageView = appBar.findViewById(R.id.drawer_header_background);

                String background = getStorageManager().getCapability(account.name).getServerBackground();
                int primaryColor = ThemeUtils.primaryColor(getAccount(), false, this);

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
        DisplayUtils.setAvatar(account, this, mCurrentAccountAvatarRadiusDimension, getResources(), avatar, this);

        if (userInfo == null || userInfo.isEmpty()) {
            setErrorMessageForMultiList(getString(R.string.userinfo_no_info_headline),
                                        getString(R.string.userinfo_no_info_text));
        } else {
            if (!TextUtils.isEmpty(userInfo.getDisplayName())) {
                fullName.setText(userInfo.getDisplayName());
            }

            emptyContentContainer.setVisibility(View.GONE);
            userInfoView.setVisibility(View.VISIBLE);

            adapter.setData(createUserInfoDetails(userInfo));
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
        addToListIfNeeded(result, R.drawable.ic_group, DisplayUtils.beautifyGroups(userInfo.getGroups()),
            R.string.user_info_groups);

        long quotaValue = userInfo.getQuota().getQuota();
        if (quotaValue > 0 || quotaValue == GetUserInfoRemoteOperation.SPACE_UNLIMITED
            || quotaValue == GetUserInfoRemoteOperation.QUOTA_LIMIT_INFO_NOT_AVAILABLE) {

            DisplayUtils.setQuotaInformation(quotaProgressBar, quotaPercentage, userInfo.getQuota(), this);
            ThemeUtils.tintDrawable(quotaIcon.getDrawable(), primaryColor);
            quotaView.setVisibility(View.VISIBLE);
        } else {
            quotaView.setVisibility(View.GONE);
        }

        return result;
    }

    private void addToListIfNeeded(List<UserInfoDetailsItem> info, @DrawableRes int icon, String text,
                                   @StringRes int contentDescriptionInt) {
        if (!TextUtils.isEmpty(text)) {
            info.add(new UserInfoDetailsItem(icon, text, getResources().getString(contentDescriptionInt), primaryColor));
        }
    }
}
