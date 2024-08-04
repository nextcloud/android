/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup;

import java.util.Comparator;

import ezvcard.VCard;

public class VCardComparator implements Comparator<VCard> {
    @Override
    public int compare(VCard o1, VCard o2) {
        String contact1 = BackupListFragment.getDisplayName(o1);
        String contact2 = BackupListFragment.getDisplayName(o2);

        return contact1.compareToIgnoreCase(contact2);
    }
}
