/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.annotation.SuppressLint;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Data holder utility to store & retrieve stuff
 */

public class DataHolderUtil {
    private Map<String, WeakReference<Object>> data = new HashMap<String, WeakReference<Object>>();

    private static DataHolderUtil instance;

    @SuppressLint("TrulyRandom")
    private SecureRandom random = new SecureRandom();

    public static synchronized DataHolderUtil getInstance() {
        if (instance == null) {
            instance = new DataHolderUtil();
        }
        return instance;
    }

    public void save(String id, Object object) {
        data.put(id, new WeakReference<Object>(object));
    }

    public Object retrieve(String id) {
        WeakReference<Object> objectWeakReference = data.get(id);
        return objectWeakReference.get();
    }

    public void delete(String id) {
        if (id != null) {
            data.remove(id);
        }
    }

    public String nextItemId() {
        String nextItemId = new BigInteger(130, random).toString(32);
        while (data.containsKey(nextItemId)) {
            nextItemId = new BigInteger(130, random).toString(32);
        }
        return nextItemId;
    }
}
