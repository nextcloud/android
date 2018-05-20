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
import android.content.res.Resources;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.status.OCCapability;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper calls for visibility logic of the sharing fragment.
 */
public class FileDetailSharingFragmentHelper {

    public static void setupHideFileListingMenuItem(MenuItem fileListing,
                                                    boolean isFolder,
                                                    boolean isEditingAllowed,
                                                    int publicSharePermissions) {
        if (!isFolder) {
            fileListing.setVisible(false);
        } else {
            if (isEditingAllowed) {
                boolean readOnly = (publicSharePermissions & OCShare.READ_PERMISSION_FLAG) != 0;
                fileListing.setChecked(!readOnly);
            } else {
                fileListing.setVisible(false);
            }
        }
    }

    public static void setupPasswordMenuItem(MenuItem password, boolean isPasswordProtected, Resources res) {
        if (isPasswordProtected) {
            password.setTitle(res.getString(
                    R.string.share_via_link_menu_password_label,
                    res.getString(R.string.share_via_link_password_title)
            ));
        } else {
            password.setTitle(res.getString(
                    R.string.share_via_link_menu_password_label,
                    res.getString(R.string.share_via_link_no_password_title)
            ));
        }
    }

    public static void setupExpirationDateMenuItem(MenuItem expirationDate, long expirationDateValue, Resources res) {
        if (expirationDateValue > 0) {
            String formattedDate =
                    SimpleDateFormat.getDateInstance().format(
                            new Date(expirationDateValue)
                    );
            expirationDate.setTitle(res.getString(
                    R.string.share_expiration_date_label,
                    formattedDate
            ));
        } else {
            expirationDate.setTitle(res.getString(
                    R.string.share_expiration_date_label,
                    res.getString(R.string.share_via_link_menu_expiration_date_never)
            ));
        }
    }

    public static void setupSearchView(SearchManager searchManager, SearchView searchView, ComponentName componentName) {
        // assumes parent activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));

        // do not iconify the widget; expand it by default
        searchView.setIconifiedByDefault(false);

        // avoid fullscreen with softkeyboard
        searchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // return true to prevent the query is processed to be queried;
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
        return (capabilities != null && capabilities.getFilesSharingPublicEnabled().isFalse());
    }

    /**
     * Fix scroll in ListView when the parent is a ScrollView
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
