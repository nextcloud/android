/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Activity displaying all server side stored activity items.
 */
public class NotificationsActivity extends FileActivity {

    private static final String TAG = NotificationsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notifications_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_notifications);
        getSupportActionBar().setTitle(getString(R.string.drawer_item_notifications));

        setupContent();
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        // TODO add all (recycler) view relevant code + data loading + adapter etc.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }

            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }
}
