/* Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.whatsnew;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.R;
import com.owncloud.android.features.FeatureItem;
import com.owncloud.android.ui.activity.PassCodeActivity;

public class WhatsNewService {

    private Resources resources;
    private AppPreferences preferences;
    private CurrentAccountProvider accountProvider;

    WhatsNewService(Resources resources,
                    AppPreferences preferences,
                    CurrentAccountProvider accountProvider) {
        this.resources = resources;
        this.preferences = preferences;
        this.accountProvider = accountProvider;
    }

    public void launchActivityIfNeeded(Activity activity) {
        if (!resources.getBoolean(R.bool.show_whats_new) || activity instanceof WhatsNewActivity) {
            return;
        }

        if (shouldShow(activity)) {
            activity.startActivity(new Intent(activity, WhatsNewActivity.class));
        }
    }

    FeatureItem[] getWhatsNew() {
        int itemVersionCode = 99999999;

        if (!isFirstRun() && BuildConfig.VERSION_CODE >= itemVersionCode
            && preferences.getLastSeenVersionCode() < itemVersionCode) {
            return new FeatureItem[0];
        } else {
            return new FeatureItem[0];
        }
    }

    private boolean shouldShow(Context callingContext) {
        return !(callingContext instanceof PassCodeActivity) && getWhatsNew().length > 0;
    }

    public boolean isFirstRun() {
        return accountProvider.getCurrentAccount() == null;
    }
}
