/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeToolbarUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar(boolean, boolean)}.
 */
public abstract class ToolbarActivity extends BaseActivity {
    protected MaterialButton mMenuButton;
    protected MaterialTextView mSearchText;
    protected MaterialButton mSwitchAccountButton;

    private AppBarLayout mAppBar;
    private RelativeLayout mDefaultToolbar;
    private Toolbar mToolbar;
    private MaterialCardView mHomeSearchToolbar;
    private ImageView mPreviewImage;
    private FrameLayout mPreviewImageContainer;
    private LinearLayout mInfoBox;
    private TextView mInfoBoxMessage;
    protected AppCompatSpinner mToolbarSpinner;
    private View mDefaultToolbarDivider;
    private ImageView mToolbarBackIcon;
    private boolean isHomeSearchToolbarShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they want
     * to use the toolbar.
     */
    private void setupToolbar(boolean isHomeSearchToolbarShow, boolean showSortListButtonGroup) {
        int fontColor = ThemeColorUtils.appBarPrimaryFontColor(this);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ThemeToolbarUtils.colorStatusBar(this);

        mAppBar = findViewById(R.id.appbar);
        mDefaultToolbar = findViewById(R.id.default_toolbar);
        mHomeSearchToolbar = findViewById(R.id.home_toolbar);
        mMenuButton = findViewById(R.id.menu_button);
        mSearchText = findViewById(R.id.search_text);
        mSwitchAccountButton = findViewById(R.id.switch_account_button);
        mDefaultToolbarDivider = findViewById(R.id.default_toolbar_divider);
        mToolbarBackIcon = findViewById(R.id.toolbar_back_icon);

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

        if (mToolbar.getOverflowIcon() != null) {
            ThemeDrawableUtils.tintDrawable(mToolbar.getOverflowIcon(), fontColor);
        }

        if (mToolbar.getNavigationIcon() != null) {
            ThemeDrawableUtils.tintDrawable(mToolbar.getNavigationIcon(), fontColor);
        }

        mToolbarBackIcon.setOnClickListener(v -> onBackPressed());

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

        title = isRoot ? ThemeUtils.getDefaultDisplayNameForRootFolder(this) : chosenFile.getFileName();
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
        /*if (isShow) {
            mAppBar.setStateListAnimator(AnimatorInflater.loadStateListAnimator(mAppBar.getContext(),
                                                                                R.animator.appbar_elevation_off));
            mDefaultToolbar.setVisibility(View.GONE);
            mHomeSearchToolbar.setVisibility(View.VISIBLE);
            ThemeToolbarUtils.colorStatusBar(this, ContextCompat.getColor(this, R.color.bg_default));
        } else {*/
        mAppBar.setStateListAnimator(AnimatorInflater.loadStateListAnimator(mAppBar.getContext(),
                                                                            R.animator.appbar_elevation_on));
        mDefaultToolbar.setVisibility(View.VISIBLE);
        mHomeSearchToolbar.setVisibility(View.GONE);
        ThemeToolbarUtils.colorStatusBar(this);
        // }
    }

    public void showHideToolbar(boolean isShow) {
        mDefaultToolbar.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public void showHideDefaultToolbarDivider(boolean isShow) {
        mDefaultToolbarDivider.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    public void updateActionBarTitleAndHomeButtonByString(String title) {
        String titleToSet = getString(R.string.app_name);    // default

        if (title != null) {
            titleToSet = title;
        }

        // set & color the chosen title
        ActionBar actionBar = getSupportActionBar();
        ThemeToolbarUtils.setColoredTitle(actionBar, titleToSet, this);

        // set home button properties
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
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
    protected final void hideInfoBox() {
        mInfoBox.setVisibility(View.GONE);
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

    /**
     * Change the bitmap for the toolbar's preview image.
     *
     * @param bitmap bitmap of the preview image
     */
    public void setPreviewImageBitmap(Bitmap bitmap) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageBitmap(bitmap);
            resetPreviewImageConfiguration();
            setPreviewImageVisibility(true);
        }
    }

    /**
     * reset preview image configuration if required the scale type and padding are changing in sharing screen so to
     * reset it call this method
     */
    private void resetPreviewImageConfiguration() {
        mPreviewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mPreviewImage.setPadding(0, 0, 0, 0);
    }

    /**
     * Change the drawable for the toolbar's preview image.
     *
     * @param drawable drawable of the preview image
     */
    public void setPreviewImageDrawable(Drawable drawable) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageDrawable(drawable);
            resetPreviewImageConfiguration();
            setPreviewImageVisibility(true);
        }
    }

    /**
     * method to show/hide the toolbar custom back icon currently this is only showing for sharing details screen for
     * thumbnail images
     *
     * @param show
     */
    public void showToolbarBackImage(boolean show) {
        ActionBar actionBar = getSupportActionBar();
        if (show) {
            mToolbarBackIcon.setVisibility(View.VISIBLE);
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        } else {
            mToolbarBackIcon.setVisibility(View.GONE);
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
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

    /**
     * method will expand the toolbar using app bar if its hidden due to scrolling
     */
    public void expandToolbar() {
        if (mAppBar != null) {
            mAppBar.setExpanded(true, true);
        }
    }

    /**
     * method will set the back icon to action bar
     * and also set the color
     * Note: This method has to be called when you are directly
     * extending activity with ToolbarActivity
     */
    public void setActionBarBackIcon(){
       ActionBar actionBar = getSupportActionBar();
       if (actionBar!=null) {
           actionBar.setDisplayHomeAsUpEnabled(true);
           Drawable backArrow = ResourcesCompat.getDrawable(
               getResources(),
               R.drawable.ic_back_arrow,
               null);

           actionBar.setHomeAsUpIndicator(
               ThemeDrawableUtils.tintDrawable(backArrow, ThemeColorUtils.appBarPrimaryFontColor(this)));
       }
    }
}
