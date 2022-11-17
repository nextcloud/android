/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud.
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
package com.owncloud.android.datamodel;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.nextcloud.client.database.NextcloudDatabase;
import com.nextcloud.client.database.dao.ArbitraryDataDao;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Database provider for handling the persistence aspects of arbitrary data table.
 */
public class ArbitraryDataProvider {
    public static final String DIRECT_EDITING = "DIRECT_EDITING";
    public static final String DIRECT_EDITING_ETAG = "DIRECT_EDITING_ETAG";
    public static final String PREDEFINED_STATUS = "PREDEFINED_STATUS";

    private static final String TRUE = "true";

    private final ArbitraryDataDao arbitraryDataDao;

    /**
     * @deprecated inject instead
     */
    @Deprecated
    public ArbitraryDataProvider(final Context context) {
        this(NextcloudDatabase.getInstance(context).arbitraryDataDao());
    }

    @Inject
    public ArbitraryDataProvider(final ArbitraryDataDao dao) {
        this.arbitraryDataDao = dao;
    }

    public void deleteKeyForAccount(String account, String key) {
        arbitraryDataDao.deleteValue(account, key);
    }

    public void storeOrUpdateKeyValue(String accountName, String key, long newValue) {
        storeOrUpdateKeyValue(accountName, key, String.valueOf(newValue));
    }

    public void storeOrUpdateKeyValue(final String accountName, final String key, final boolean newValue) {
        storeOrUpdateKeyValue(accountName, key, String.valueOf(newValue));
    }

    public void storeOrUpdateKeyValue(@NonNull String accountName,
                                      @NonNull String key,
                                      @Nullable String newValue) {
        final String currentValue = arbitraryDataDao.getValue(accountName, key);
        if (currentValue != null) {
            arbitraryDataDao.updateValue(accountName, key, newValue);
        } else {
            arbitraryDataDao.insertValue(accountName, key, newValue);
        }
    }

    Long getLongValue(String accountName, String key) {
        String value = getValue(accountName, key);

        if (value.isEmpty()) {
            return -1L;
        } else {
            return Long.valueOf(value);
        }
    }


    public Long getLongValue(User user, String key) {
        return getLongValue(user.getAccountName(), key);
    }

    public boolean getBooleanValue(String accountName, String key) {
        return TRUE.equalsIgnoreCase(getValue(accountName, key));
    }

    public boolean getBooleanValue(User user, String key) {
        return getBooleanValue(user.getAccountName(), key);
    }

    /**
     * returns integer if found else -1
     *
     * @param accountName name of account
     * @param key key to get value for
     * @return Integer specified by account and key
     */
    public Integer getIntegerValue(String accountName, String key) {
        String value = getValue(accountName, key);

        if (value.isEmpty()) {
            return -1;
        } else {
            return Integer.valueOf(value);
        }
    }

    /**
     * Returns stored value as string or empty string
     *
     * @return string if value found or empty string
     */
    @NonNull
    public String getValue(@Nullable User user, String key) {
        return user != null ? getValue(user.getAccountName(), key) : "";
    }

    public String getValue(String accountName, String key) {
        final String value = arbitraryDataDao.getValue(accountName, key);
        if (value == null) {
            return "";
        }
        return value;
    }
}
