/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files.services;

/**
 * Defines how to handle file name collisions during uploads.
 *
 * <p><b>Important:</b> Enum ordinals are stored directly in the database.
 * Do <b>not</b> change their order or remove constants to avoid breaking
 * compatibility with old data.</p>
 *
 * <p><b>Database value mapping:</b></p>
 * <ul>
 *   <li>0 → {@link #RENAME} (old forceOverwrite = false)</li>
 *   <li>1 → {@link #OVERWRITE} (old forceOverwrite = true)</li>
 *   <li>2 → {@link #SKIP}</li>
 *   <li>3 → {@link #ASK_USER}</li>
 * </ul>
 */
public enum NameCollisionPolicy {
    RENAME,
    OVERWRITE,
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
