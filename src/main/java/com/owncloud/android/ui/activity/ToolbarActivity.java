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

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.ThemeUtils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar()}.
 */
public abstract class ToolbarActivity extends BaseActivity {
    private AppBarLayout mAppBar;
    private RelativeLayout mDefaultToolbar;
    private MaterialCardView mHomeToolbar;
    protected MaterialButton mMenuButton;
    private MaterialTextView mSearchText;
    protected MaterialButton mSwitchAccountButton;
    private ProgressBar mProgressBar;
    private ImageView mPreviewImage;
    private FrameLayout mPreviewImageContainer;
    private LinearLayout mInfoBox;
    private TextView mInfoBoxMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they want
     * to use the toolbar.
     */
    protected void setupToolbar(boolean useBackgroundImage) {
        int primaryColor = ThemeUtils.primaryAppbarColor(this);
        int fontColor = ThemeUtils.appBarPrimaryFontColor(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAppBar = findViewById(R.id.appbar);
        mDefaultToolbar = findViewById(R.id.default_toolbar);
        mHomeToolbar = findViewById(R.id.home_toolbar);
        mMenuButton = findViewById(R.id.menu_button);
        mSearchText = findViewById(R.id.search_text);
        mSwitchAccountButton = findViewById(R.id.switch_account_button);

        if (mAppBar != null) {
            mAppBar.setElevation(getResources().getDimension(R.dimen.default_appbar_elevation));
        }

        mProgressBar = findViewById(R.id.toolbar_progressBar);
        setProgressBarBackgroundColor();

        mInfoBox = findViewById(R.id.info_box);
        mInfoBoxMessage = findViewById(R.id.info_box_message);

        mPreviewImage = findViewById(R.id.preview_image);
        mPreviewImageContainer = findViewById(R.id.preview_image_frame);

        ThemeUtils.colorStatusBar(this);

        if (toolbar.getOverflowIcon() != null) {
            ThemeUtils.tintDrawable(toolbar.getOverflowIcon(), fontColor);
        }

        if (toolbar.getNavigationIcon() != null) {
            ThemeUtils.tintDrawable(toolbar.getNavigationIcon(), fontColor);
        }

        if (!useBackgroundImage) {
            toolbar.setBackgroundColor(primaryColor);
        }
    }

    public void setupToolbar() {
        setupToolbar(false);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title;
        boolean isRoot = isRoot(chosenFile);

        title = isRoot ? ThemeUtils.getDefaultDisplayNameForRootFolder(this) : chosenFile.getFileName();
        updateActionBarTitleAndHomeButtonByString(title);

        if (isRoot) {
            mAppBar.setElevation(0);
            mDefaultToolbar.setVisibility(View.GONE);
            mHomeToolbar.setVisibility(View.VISIBLE);
            mSearchText.setText(String.format("Search in %s", title));
        } else {
            mAppBar.setElevation(getResources().getDimension(R.dimen.default_appbar_elevation));
            mDefaultToolbar.setVisibility(View.VISIBLE);
            mHomeToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButtonByString(String title) {
        String titleToSet = getString(R.string.app_name);    // default

        if (title != null) {
            titleToSet = title;
        }

        // set & color the chosen title
        ActionBar actionBar = getSupportActionBar();
        ThemeUtils.setColoredTitle(actionBar, titleToSet, this);

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

    /**
     * Change the visibility for the toolbar's progress bar.
     *
     * @param isVisible visibility of the progress bar
     */
    public void showProgressBar(boolean isVisible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Change the visibility for the toolbar's preview image.
     *
     * @param visibility visibility of the preview image
     */
    public void setPreviewImageVisibility(int visibility) {
        if (mPreviewImage != null && mPreviewImageContainer != null) {
            mPreviewImageContainer.setVisibility(visibility);
        }
    }

    /**
     * Change the bitmap for the toolbar's preview image.
     *
     * @param bitmap bitmap of the preview image
     */
    public void setPreviewImageBitmap(Bitmap bitmap) {
        if (mPreviewImage != null) {
            mPreviewImage.setImageBitmap(bitmap);
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
        }
    }

    /**
     * get the toolbar's preview image view.
     */
    public ImageView getPreviewImageView() {
        return mPreviewImage;
    }

    /**
     * Set the background to to progress bar of the toolbar. The resource should refer to a Drawable object or 0 to
     * remove the background.#
     */
    private void setProgressBarBackgroundColor() {
        if (mProgressBar != null) {
            mProgressBar.setBackgroundColor(ThemeUtils.primaryAppbarColor(this));
            mProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryColor(this), PorterDuff.Mode.SRC_IN);
        }
    }
}
