/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files.services;

/**
 * Ordinal of enumerated constants is important for old data compatibility.
 */
public enum NameCollisionPolicy {
    RENAME, // Ordinal corresponds to old forceOverwrite = false (0 in database)
    OVERWRITE, // Ordinal corresponds to old forceOverwrite = true (1 in database)
    SKIP,
    ASK_USER;

    public static final NameCollisionPolicy DEFAULT = RENAME;

    public static NameCollisionPolicy deserialize(int ordinal) {
        NameCollisionPolicy[] values = NameCollisionPolicy.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DEFAULT;
    }

    public int serialize() {
        return this.ordinal();
    }
}
