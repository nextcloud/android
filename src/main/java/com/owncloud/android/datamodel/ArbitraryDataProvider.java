/**
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
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.ArrayList;

/**
 * Database provider for handling the persistence aspects of arbitrary data table.
 */

public class ArbitraryDataProvider {
    static private final String TAG = ArbitraryDataProvider.class.getSimpleName();

    private ContentResolver contentResolver;

    public ArbitraryDataProvider(ContentResolver contentResolver) {
        if (contentResolver == null) {
            throw new IllegalArgumentException("Cannot create an instance with a NULL contentResolver");
        }
        this.contentResolver = contentResolver;
    }

    public int deleteKeyForAccount(String account, String key) {
        return contentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " = ? AND " +
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY + "= ?",
                new String[]{account, key}
        );
    }

    public int deleteForKeyWhereAccountNotIn(ArrayList<String> accounts, String key) {
        return contentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " NOT IN (?) AND " +
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY + "= ?",
                new String[]{String.valueOf(accounts), key}
        );
    }


    public void storeOrUpdateKeyValue(String accountName, String key, String newValue) {
        ArbitraryDataSet data = getArbitraryDataSet(accountName, key);
        if (data == null) {
            Log_OC.v(TAG, "Adding arbitrary data with cloud id: " + accountName + " key: " + key
                    + " value: " + newValue);
            ContentValues cv = new ContentValues();
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID, accountName);
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY, key);
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_VALUE, newValue);

            Uri result = contentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA, cv);

            if (result == null) {
                Log_OC.v(TAG, "Failed to store arbitrary data with cloud id: " + accountName + " key: " + key
                        + " value: " + newValue);
            }
        } else {
            Log_OC.v(TAG, "Updating arbitrary data with cloud id: " + accountName + " key: " + key
                    + " value: " + newValue);
            ContentValues cv = new ContentValues();
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID, data.getCloudId());
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY, data.getKey());
            cv.put(ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_VALUE, newValue);

            int result = contentResolver.update(
                    ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                    cv,
                    ProviderMeta.ProviderTableMeta._ID + "=?",
                    new String[]{String.valueOf(data.getId())}
            );

            if (result == 0) {
                Log_OC.v(TAG, "Failed to update arbitrary data with cloud id: " + accountName + " key: " + key
                        + " value: " + newValue);
            }
        }
    }


    public Long getLongValue(Account account, String key) {
        String value = getValue(account, key);

        if (value.isEmpty()) {
            return -1l;
        } else {
            return Long.valueOf(value);
        }
    }

    public boolean getBooleanValue(String accountName, String key) {
        String value = getValue(accountName, key);

        return !value.isEmpty() && value.equalsIgnoreCase("true");
    }

    public boolean getBooleanValue(Account account, String key) {
        return getBooleanValue(account.name, key);
    }

    /**
     * returns integer if found else -1
     *
     * @param accountName
     * @param key
     * @return
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
     * returns integer if found else -1
     *
     * @param account
     * @param key
     * @return
     */
    public Integer getIntegerValue(Account account, String key) {
        return getIntegerValue(account.name, key);
    }

    /**
     * Returns stored value as string or empty string
     * @return string if value found or empty string
     */
    public String getValue(Account account, String key) {
        return getValue(account.name, key);
    }

    public String getValue(String accountName, String key) {
        Cursor cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                null,
                ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " = ? and " +
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY + " = ?",
                new String[]{accountName, key},
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String value = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_VALUE));
                if (value == null) {
                    Log_OC.e(TAG, "Arbitrary value could not be created from cursor");
                } else {
                    return value;
                }
            }
            cursor.close();
            return "";
        } else {
            Log_OC.e(TAG, "DB error restoring arbitrary values.");
        }

        return "";
    }

    private ArbitraryDataSet getArbitraryDataSet(String accountName, String key) {
        Cursor cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                null,
                ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " = ? and " +
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY + " = ?",
                new String[]{accountName, key},
                null
        );

        ArbitraryDataSet dataSet = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
                String dbAccount = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID));
                String dbKey = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_KEY));
                String dbValue = cursor.getString(cursor.getColumnIndex(
                        ProviderMeta.ProviderTableMeta.ARBITRARY_DATA_VALUE));

                if (id == -1) {
                    Log_OC.e(TAG, "Arbitrary value could not be created from cursor");
                } else {
                    dataSet = new ArbitraryDataSet(id, dbAccount, dbKey, dbValue);
                }
            }
            cursor.close();
        } else {
            Log_OC.e(TAG, "DB error restoring arbitrary values.");
        }

        return dataSet;
    }


    public class ArbitraryDataSet {
        private int id;
        private String cloudId;
        private String key;
        private String value;

        public ArbitraryDataSet(int id, String cloudId, String key, String value) {
            this.id = id;
            this.cloudId = cloudId;
            this.key = key;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public String getCloudId() {
            return cloudId;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

}
