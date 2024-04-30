/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
