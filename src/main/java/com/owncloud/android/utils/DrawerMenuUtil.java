/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.content.res.Resources;
import android.view.Menu;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import androidx.annotation.Nullable;

/**
 * A helper class for drawer menu related operations.
 */
public final class DrawerMenuUtil {
    private DrawerMenuUtil() {
    }

    public static void filterForBottomToolbarMenuItems(Menu menu, Resources resources) {
        if (resources.getBoolean(R.bool.bottom_toolbar_enabled)) {
            filterMenuItems(menu, R.id.nav_all_files, R.id.nav_settings, R.id.nav_favorites, R.id.nav_photos);
        }
    }

    public static void filterSearchMenuItems(Menu menu,
                                             Account account,
                                             Resources resources,
                                             boolean hasSearchSupport) {
        if (account != null && !hasSearchSupport) {
            filterMenuItems(menu, R.id.nav_photos, R.id.nav_favorites, R.id.nav_videos);
        }

        if (hasSearchSupport) {
            if (!resources.getBoolean(R.bool.recently_added_enabled)) {
                menu.removeItem(R.id.nav_recently_added);
            }

            if (!resources.getBoolean(R.bool.recently_modified_enabled)) {
                menu.removeItem(R.id.nav_recently_modified);
            }

            if (!resources.getBoolean(R.bool.videos_enabled)) {
                menu.removeItem(R.id.nav_videos);
            }
        } else if (account != null) {
            filterMenuItems(menu, R.id.nav_recently_added, R.id.nav_recently_modified, R.id.nav_videos);
        }
    }

    public static void filterTrashbinMenuItem(Menu menu,
                                              @Nullable Account account,
                                              @Nullable OCCapability capability,
                                              UserAccountManager accountManager) {
        if (account != null && capability != null &&
                (accountManager.getServerVersion(account).compareTo(OwnCloudVersion.nextcloud_14) < 0 ||
                        capability.getFilesUndelete().isFalse() || capability.getFilesUndelete().isUnknown())) {
            filterMenuItems(menu, R.id.nav_trashbin);
        }
    }

    public static void filterActivityMenuItem(Menu menu, @Nullable OCCapability capability) {
        if (capability != null && capability.getActivity().isFalse()) {
            filterMenuItems(menu, R.id.nav_activity);
        }
    }

    public static void removeMenuItem(Menu menu, int id, boolean remove) {
        if (remove) {
            menu.removeItem(id);
        }
    }

    public static void setupHomeMenuItem(Menu menu, Resources resources) {
        if (resources.getBoolean(R.bool.use_home) && menu.findItem(R.id.nav_all_files) != null) {
            menu.findItem(R.id.nav_all_files).setTitle(resources.getString(R.string.drawer_item_home));
            menu.findItem(R.id.nav_all_files).setIcon(R.drawable.ic_home);
        }
    }

    private static void filterMenuItems(Menu menu, int... menuIds) {
        if (menuIds != null) {
            for (int menuId : menuIds) {
                menu.removeItem(menuId);
            }
        }
    }
}
