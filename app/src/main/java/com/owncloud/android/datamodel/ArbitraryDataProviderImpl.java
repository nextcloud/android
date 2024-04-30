/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel;

import android.content.Context;

import com.nextcloud.client.account.User;
import com.nextcloud.client.database.NextcloudDatabase;
import com.nextcloud.client.database.dao.ArbitraryDataDao;
import com.nextcloud.client.database.entity.ArbitraryDataEntity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Database provider for handling the persistence aspects of arbitrary data table.
 * <p>
 * Don't instantiate this class, inject the interface instead.
 */
public class ArbitraryDataProviderImpl implements ArbitraryDataProvider {

    private static final String TRUE = "true";

    private final ArbitraryDataDao arbitraryDataDao;

    /**
     * @deprecated inject interface instead
     */
    @Deprecated
    public ArbitraryDataProviderImpl(final Context context) {
        this(NextcloudDatabase.getInstance(context).arbitraryDataDao());
    }

    public ArbitraryDataProviderImpl(@NonNull final ArbitraryDataDao dao) {
        this.arbitraryDataDao = dao;
    }

    @Override
    public void deleteKeyForAccount(@NonNull String account, @NonNull String key) {
        arbitraryDataDao.deleteValue(account, key);
    }

    @Override
    public void storeOrUpdateKeyValue(@NonNull String accountName, @NonNull String key, long newValue) {
        storeOrUpdateKeyValue(accountName, key, String.valueOf(newValue));
    }

    @Override
    public void incrementValue(@NonNull String accountName, @NonNull String key) {
        int oldValue = getIntegerValue(accountName, key);

        int value = 1;
        if (oldValue > 0) {
            value = oldValue + 1;
        }
        storeOrUpdateKeyValue(accountName, key, value);
    }

    @Override
    public void storeOrUpdateKeyValue(@NonNull final String accountName, @NonNull final String key, final boolean newValue) {
        storeOrUpdateKeyValue(accountName, key, String.valueOf(newValue));
    }

    @Override
    public void storeOrUpdateKeyValue(@NonNull String accountName,
                                      @NonNull String key,
                                      @Nullable String newValue) {
        final ArbitraryDataEntity currentValue = arbitraryDataDao.getByAccountAndKey(accountName, key);
        if (currentValue != null) {
            arbitraryDataDao.updateValue(accountName, key, newValue);
        } else {
            arbitraryDataDao.insertValue(accountName, key, newValue);
        }
    }

    @Override
    public void storeOrUpdateKeyValue(@NonNull User user,
                                      @NonNull String key,
                                      @NonNull String newValue) {
        storeOrUpdateKeyValue(user.getAccountName(), key, newValue);
    }

    @Override
    public long getLongValue(@NonNull String accountName, @NonNull String key) {
        String value = getValue(accountName, key);

        if (value.isEmpty()) {
            return -1L;
        } else {
            return Long.parseLong(value);
        }
    }


    @Override
    public long getLongValue(User user, @NonNull String key) {
        return getLongValue(user.getAccountName(), key);
    }

    @Override
    public boolean getBooleanValue(@NonNull String accountName, @NonNull String key) {
        return TRUE.equalsIgnoreCase(getValue(accountName, key));
    }

    @Override
    public boolean getBooleanValue(User user, @NonNull String key) {
        return getBooleanValue(user.getAccountName(), key);
    }

    /**
     * returns integer if found else -1
     *
     * @param accountName name of account
     * @param key         key to get value for
     * @return Integer specified by account and key
     */
    @Override
    public int getIntegerValue(@NonNull String accountName, @NonNull String key) {
        String value = getValue(accountName, key);

        if (value.isEmpty()) {
            return -1;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * Returns stored value as string or empty string
     *
     * @return string if value found or empty string
     */
    @Override
    @NonNull
    public String getValue(@Nullable User user, @NonNull String key) {
        return user != null ? getValue(user.getAccountName(), key) : "";
    }

    @Override
    @NonNull
    public String getValue(@NonNull String accountName, @NonNull String key) {
        final ArbitraryDataEntity entity = arbitraryDataDao.getByAccountAndKey(accountName, key);
        if (entity == null || entity.getValue() == null) {
            return "";
        }
        return entity.getValue();
    }
}
