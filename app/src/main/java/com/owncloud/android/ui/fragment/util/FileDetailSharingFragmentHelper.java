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

package com.owncloud.android.ui.fragment.util;

import android.app.SearchManager;
import android.content.ComponentName;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.owncloud.android.lib.resources.status.OCCapability;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

/**
 * Helper calls for visibility logic of the sharing fragment.
 */
public final class FileDetailSharingFragmentHelper {
    private FileDetailSharingFragmentHelper() {
        // Private empty constructor
    }

    /**
     * sets up the {@link SearchView}.
     *
     * @param searchManager the {@link SearchManager}
     * @param searchView    the {@link SearchView}
     * @param componentName the {@link ComponentName}
     */
    public static void setupSearchView(@Nullable SearchManager searchManager, SearchView searchView,
                                       ComponentName componentName) {
        if (searchManager == null) {
            searchView.setVisibility(View.GONE);
            return;
        }

        // assumes parent activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));

        // do not iconify the widget; expand it by default
        searchView.setIconifiedByDefault(false);

        // avoid fullscreen with softkeyboard
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // return true to prevent the query from being processed;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // leave it for the parent listener in the hierarchy / default behaviour
                return false;
            }
        });
    }

    /**
     * @return 'True' when public share is disabled in the server.
     */
    public static boolean isPublicShareDisabled(OCCapability capabilities) {
        return capabilities != null && capabilities.getFilesSharingPublicEnabled().isFalse();
    }
}
