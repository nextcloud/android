/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.annotation.SuppressLint;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Data holder utility to store & retrieve stuff
 */

public class DataHolderUtil {
    private Map<String, WeakReference<Object>> data = new HashMap<>();

    private static DataHolderUtil instance;

    @SuppressLint("TrulyRandom")
    private SecureRandom random = new SecureRandom();

    @SuppressFBWarnings("MS_EXPOSE_REP")
    public static synchronized DataHolderUtil getInstance() {
        if (instance == null) {
            instance = new DataHolderUtil();
        }
        return instance;
    }

    public void save(String id, Object object) {
        data.put(id, new WeakReference<>(object));
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
