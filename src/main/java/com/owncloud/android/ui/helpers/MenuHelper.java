package com.owncloud.android.ui.helpers;

import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;

import com.owncloud.android.R;

/**
 * Collection of helper functions for handling menus.
 */
public final class MenuHelper {

    private MenuHelper() {

    }

    /**
     * Inflates the item file menu and handles menu items according to SDK version.
     *
     * @param inflater The inflater to use for inflation of the item file menu.
     * @param menu The menu into which to inflate.
     */
    public static void inflateItemFileMenu(final MenuInflater inflater, final Menu menu) {
        inflater.inflate(R.menu.item_file, menu);
        menu.findItem(R.id.action_add_shortcut_to_homescreen).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O);
    }
}
