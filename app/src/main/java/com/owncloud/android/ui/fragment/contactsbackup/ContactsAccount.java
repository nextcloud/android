/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        if (obj instanceof ContactsAccount) {
            ContactsAccount other = (ContactsAccount) obj;
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
