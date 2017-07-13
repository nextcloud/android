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

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.widget.ProgressBar;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.ThemeUtils;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar()}.
 */
public abstract class ToolbarActivity extends BaseActivity {
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they
     * want to use the toolbar.
     */
    protected void setupToolbar(boolean useBackgroundImage) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        if (mProgressBar != null) {
            mProgressBar.setIndeterminateDrawable(
                    ContextCompat.getDrawable(this, R.drawable.actionbar_progress_indeterminate_horizontal));

            ThemeUtils.colorToolbarProgressBar(this, ThemeUtils.primaryColor());
        }

        ThemeUtils.colorStatusBar(this, ThemeUtils.primaryDarkColor());

        if (toolbar.getOverflowIcon() != null) {
            ThemeUtils.tintDrawable(toolbar.getOverflowIcon(), ThemeUtils.fontColor());
        }

        if (toolbar.getNavigationIcon() != null) {
            ThemeUtils.tintDrawable(toolbar.getNavigationIcon(), ThemeUtils.fontColor());
        }

        if (!useBackgroundImage) {
            toolbar.setBackgroundColor(ThemeUtils.primaryColor());
        }
    }

    protected void setupToolbar() {
        setupToolbar(false);
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title = ThemeUtils.getDefaultDisplayNameForRootFolder();    // default
        boolean inRoot;

        // choose the appropriate title
        inRoot = (
                chosenFile == null ||
                        (chosenFile.isFolder() && chosenFile.getParentId() == FileDataStorageManager.ROOT_PARENT_ID)
        );
        if (!inRoot) {
            title = chosenFile.getFileName();
        }

        updateActionBarTitleAndHomeButtonByString(title);
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
        ThemeUtils.setColoredTitle(actionBar, titleToSet);

        // set home button properties
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null && toolbar.getNavigationIcon() != null) {
            ThemeUtils.tintDrawable(toolbar.getNavigationIcon(), ThemeUtils.fontColor());
        }
    }

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return <code>true</code> if it is <code>null</code> or the root folder, else returns <code>false</code>
     */
    public boolean isRoot(OCFile file) {
        return file == null ||
                (file.isFolder() && file.getParentId() == FileDataStorageManager.ROOT_PARENT_ID);
    }

    /**
     * Change the indeterminate mode for the toolbar's progress bar.
     *
     * @param indeterminate <code>true</code> to enable the indeterminate mode
     */
    public void setIndeterminate(boolean indeterminate) {
        mProgressBar.setIndeterminate(indeterminate);
    }

    /**
     * Set the background to to progress bar of the toolbar. The resource should refer to
     * a Drawable object or 0 to remove the background.#
     *
     * @param color The identifier of the color.
     */
    public void setProgressBarBackgroundColor(@ColorInt int color) {
        mProgressBar.setBackgroundColor(color);
        mProgressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }
}
