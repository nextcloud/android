/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.theme;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CapabilityUtils {
    private static final Map<String, OCCapability> cachedCapabilities = new HashMap<>();

    public static OCCapability getCapability(Context context) {
        User user = null;
        if (context != null) {
            // TODO: refactor when dark theme work is completed
            user = UserAccountManagerImpl.fromContext(context).getUser();
        }

        if (user != null) {
            return getCapability(user, context);
        } else {
            return new OCCapability();
        }
    }

    /**
     * @deprecated use {@link #getCapability(User, Context)} instead
     */
    @Deprecated
    public static OCCapability getCapability(Account acc, Context context) {
        User user = null;

        if (acc != null) {
            user = UserAccountManagerImpl.fromContext(context).getUser(acc.name);
        } else if (context != null) {
            // TODO: refactor when dark theme work is completed
            user = UserAccountManagerImpl.fromContext(context).getUser();
        }

        if (user != null) {
            return getCapability(user, context);
        } else {
            return new OCCapability();
        }
    }

    public static OCCapability getCapability(User user, Context context) {
        OCCapability capability = cachedCapabilities.get(user.getAccountName());

        if (capability == null) {
            FileDataStorageManager storageManager = new FileDataStorageManager(user, context.getContentResolver());
            capability = storageManager.getCapability(user.getAccountName());

            cachedCapabilities.put(capability.getAccountName(), capability);
        }

        return capability;
    }

    public static void updateCapability(OCCapability capability) {
        cachedCapabilities.put(capability.getAccountName(), capability);
    }

    public static boolean checkOutdatedWarning(Resources resources,
                                               OwnCloudVersion version,
                                               boolean hasExtendedSupport) {
        return resources.getBoolean(R.bool.show_outdated_server_warning) &&
            (MainApp.OUTDATED_SERVER_VERSION.isSameMajorVersion(version) ||
                version.isOlderThan(MainApp.OUTDATED_SERVER_VERSION))
            && !hasExtendedSupport;
    }
}
