package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;

/**
 * Base class providing toolbar registration functionality, see {@link #setupToolbar()}.
 */
public class ToolbarActivity extends AppCompatActivity {
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Toolbar setup that must be called in implementer's {@link #onCreate} after {@link #setContentView} if they
     * want to use the toolbar.
     */
    protected void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setIndeterminateDrawable(
                ContextCompat.getDrawable(this, R.drawable.actionbar_progress_indeterminate_horizontal));
    }

    /**
     * Updates title bar and home buttons (state and icon).
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        String title = getString(R.string.default_display_name_for_root_folder);    // default
        boolean inRoot;

        // choose the appropiate title
        inRoot = (
                chosenFile == null ||
                        (chosenFile.isFolder() && chosenFile.getParentId() == FileDataStorageManager.ROOT_PARENT_ID)
        );
        if (!inRoot) {
            title = chosenFile.getFileName();
        }

        /// set the chosen title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(title);

        /// also as content description
        View actionBarTitleView = getWindow().getDecorView().findViewById(
                getResources().getIdentifier("action_bar_title", "id", "android")
        );
        // TODO remove legacy code
        if (actionBarTitleView != null) {    // it's null in Android 2.x
            actionBarTitleView.setContentDescription(title);
        }

        /// set home button properties
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
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
}
