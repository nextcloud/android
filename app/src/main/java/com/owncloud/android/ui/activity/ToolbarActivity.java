/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 ZetaTom
 * SPDX-FileCopyrightText: 2023 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-FileCopyrightText: 2022 Brey Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 TSI-mc
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2016-2022 Andy Scherzinger
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatSpinner;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar(boolean, boolean)}.
 */
public abstract class ToolbarActivity extends BaseActivity implements Injectable {
    protected MaterialButton mMenuButton;
    protected MaterialTextView mSearchText;
    protected MaterialButton mSwitchAccountButton;

    private AppBarLayout mAppBar;
    private RelativeLayout mDefaultToolbar;
    private MaterialToolbar mToolbar;
    private MaterialCardView mHomeSearchToolbar;
    private ImageView mPreviewImage;
    private FrameLayout mPreviewImageContainer;
    private LinearLayout mInfoBox;
    private TextView mInfoBoxMessage;
    protected AppCompatSpinner mToolbarSpinner;
    private boolean isHomeSearchToolbarShow = false;

    @Inject public ThemeColorUtils themeColorUtils;
    @Inject public ThemeUtils themeUtils;
    @Inject public ViewThemeUtils viewThemeUtils;

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they want
     * to use the toolbar.
     */
    private void setupToolbar(boolean isHomeSearchToolbarShow, boolean showSortListButtonGroup) {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mAppBar = findViewById(R.id.appbar);
        mDefaultToolbar = findViewById(R.id.default_toolbar);
        mHomeSearchToolbar = findViewById(R.id.home_toolbar);
        mMenuButton = findViewById(R.id.menu_button);
        mSearchText = findViewById(R.id.search_text);
        mSwitchAccountButton = findViewById(R.id.switch_account_button);

        if (showSortListButtonGroup) {
            findViewById(R.id.sort_list_button_group).setVisibility(View.VISIBLE);
        }

        this.isHomeSearchToolbarShow = isHomeSearchToolbarShow;
        updateActionBarTitleAndHomeButton(null);

        mInfoBox = findViewById(R.id.info_box);
        mInfoBoxMessage = findViewById(R.id.info_box_message);

        mPreviewImage = findViewById(R.id.preview_image);
        mPreviewImageContainer = findViewById(R.id.preview_image_frame);

        mToolbarSpinner = findViewById(R.id.toolbar_spinner);

        viewThemeUtils.material.themeToolbar(mToolbar);
        viewThemeUtils.material.colorToolbarOverflowIcon(mToolbar);
        viewThemeUtils.platform.themeStatusBar(this);
        viewThemeUtils.material.colorMaterialTextButton(mSwitchAccountButton);
    }

    public void setupToolbarShowOnlyMenuButtonAndTitle(String title, View.OnClickListener toggleDrawer) {
        setupToolbar(false, false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        LinearLayout toolbar = findViewById(R.id.toolbar_linear_layout);
        MaterialButton menuButton = findViewById(R.id.toolbar_menu_button);
        MaterialTextView titleTextView = findViewById(R.id.toolbar_title);
        titleTextView.setText(title);
        toolbar.setVisibility(View.VISIBLE);
        menuButton.setOnClickListener(toggleDrawer);
    }

    public void setupToolbar() {
        setupToolbar(false, false);
    }

    public void setupHomeSearchToolbarWithSortAndListButtons() {
        setupToolbar(true, true);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title;
        boolean isRoot = isRoot(chosenFile);

        title = isRoot ? themeUtils.getDefaultDisplayNameForRootFolder(this) : chosenFile.getFileName();
        updateActionBarTitleAndHomeButtonByString(title);

        if (mAppBar != null) {
            showHomeSearchToolbar(title, isRoot);
        }
    }

    public void showSearchView() {
        if (isHomeSearchToolbarShow) {
            showHomeSearchToolbar(false);
        }
    }

    public void hideSearchView(OCFile chosenFile) {
        if (isHomeSearchToolbarShow) {
            showHomeSearchToolbar(isRoot(chosenFile));
        }
    }

    private void showHomeSearchToolbar(String title, boolean isRoot) {
        showHomeSearchToolbar(isHomeSearchToolbarShow && isRoot);
        mSearchText.setText(getString(R.string.appbar_search_in, title));
    }

    @SuppressLint("PrivateResource")
    private void showHomeSearchToolbar(boolean isShow) {
        viewThemeUtils.material.themeToolbar(mToolbar);
        if (isShow) {
            viewThemeUtils.platform.resetStatusBar(this);
            mAppBar.setStateListAnimator(AnimatorInflater.loadStateListAnimator(mAppBar.getContext(),
                                                                                R.animator.appbar_elevation_off));
            mDefaultToolbar.setVisibility(View.GONE);
            mHomeSearchToolbar.setVisibility(View.VISIBLE);
            viewThemeUtils.material.themeCardView(mHomeSearchToolbar);
            viewThemeUtils.material.themeSearchBarText(mSearchText);
        } else {
            mAppBar.setStateListAnimator(AnimatorInflater.loadStateListAnimator(mAppBar.getContext(),
                                                                                R.animator.appbar_elevation_on));
            viewThemeUtils.platform.themeStatusBar(this);
            mDefaultToolbar.setVisibility(View.VISIBLE);
            mHomeSearchToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    public void updateActionBarTitleAndHomeButtonByString(String title) {
        // set & color the chosen title
        ActionBar actionBar = getSupportActionBar();

        // set home button properties
        if (actionBar != null) {
            if (title != null) {
                actionBar.setTitle(title);
                actionBar.setDisplayShowTitleEnabled(true);
            } else {
                actionBar.setDisplayShowTitleEnabled(false);
            }
        }
    }

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return <code>true</code> if it is <code>null</code> or the root folder, else returns <code>false</code>
     */
    public boolean isRoot(OCFile file) {

        return file == null || (file.isFolder() && file.getParentId() == FileDataStorageManager.ROOT_PARENT_ID);
    }

    /**
     * shows the toolbar's info box with the given text.
     *
     * @param text the text to be displayed
     */
    protected final void showInfoBox(@StringRes int text) {
        mInfoBox.setVisibility(View.VISIBLE);
        mInfoBoxMessage.setText(text);
    }

    /**
     * Hides the toolbar's info box.
     */
    public final void hideInfoBox() {
        if (mInfoBox != null) {
            mInfoBox.setVisibility(View.GONE);
        }
    }

    public void setPreviewImageVisibility(boolean isVisibility) {
        if (mPreviewImage != null && mPreviewImageContainer != null) {
            if (isVisibility) {
                mToolbar.setTitle(null);
                mToolbar.setBackgroundColor(Color.TRANSPARENT);
            } else {
                mToolbar.setBackgroundResource(R.color.appbar);
            }
            mPreviewImageContainer.setVisibility(isVisibility ? View.VISIBLE : View.GONE);
        }
    }

    public void hidePreviewImage() {
        setPreviewImageVisibility(false);
    }

    public void showSortListGroup(boolean show) {
        findViewById(R.id.sort_list_button_group).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public boolean sortListGroupVisibility(){
        return findViewById(R.id.sort_list_button_group).getVisibility() == View.VISIBLE;
    }
    /**
     * Change the bitmap for the toolbar's preview image.
     *
     * @param bitmap bitmap of the preview image
     */
    public void setPreviewImageBitmap(Bitmap bitmap) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageBitmap(bitmap);
            setPreviewImageVisibility(true);
        }
    }

    /**
     * Change the drawable for the toolbar's preview image.
     *
     * @param drawable drawable of the preview image
     */
    public void setPreviewImageDrawable(Drawable drawable) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageDrawable(drawable);
            setPreviewImageVisibility(true);
        }
    }

    /**
     * get the toolbar's preview image view.
     */
    public ImageView getPreviewImageView() {
        return mPreviewImage;
    }

    public FrameLayout getPreviewImageContainer() {
        return mPreviewImageContainer;
    }

    public void updateToolbarSubtitle(@NonNull String subtitle) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
            viewThemeUtils.androidx.themeActionBarSubtitle(this, actionBar);
        }
    }

    public void clearToolbarSubtitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(null);
        }
    }
}
