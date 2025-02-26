/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class ContactsAccount {
    private final String displayName;
    private final String name;
    private final String type;

    ContactsAccount(String displayName, String name, String type) {
        this.displayName = displayName;
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ContactsAccount other) {
            return this.name.equalsIgnoreCase(other.name) && this.type.equalsIgnoreCase(other.type);
        } else {
            return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{displayName, name, type});
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
