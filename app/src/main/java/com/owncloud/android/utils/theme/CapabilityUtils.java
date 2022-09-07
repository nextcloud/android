/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH
 * Copyright (C) 2018 Andy Scherzinger
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
package com.owncloud.android.utils.theme;

import android.accounts.Account;
import android.content.Context;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.resources.status.OCCapability;

import java.util.HashMap;
import java.util.Map;

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
        Optional<User> user = Optional.empty();

        if (acc != null) {
            user = UserAccountManagerImpl.fromContext(context).getUser(acc.name);
        } else if (context != null) {
            // TODO: refactor when dark theme work is completed
            user = Optional.of(UserAccountManagerImpl.fromContext(context).getUser());
        }

        if (user.isPresent()) {
            return getCapability(user.get(), context);
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
}
