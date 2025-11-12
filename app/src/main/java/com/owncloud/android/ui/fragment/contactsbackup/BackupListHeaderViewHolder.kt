/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup

import android.content.Context
import android.widget.ArrayAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.owncloud.android.databinding.BackupListItemHeaderBinding

class BackupListHeaderViewHolder(val binding: BackupListItemHeaderBinding, val context: Context) :
    SectionedViewHolder(binding.root) {
    val adapter = ArrayAdapter<ContactsAccount?>(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        ArrayList()
    )

    init {
        binding.spinner.adapter = adapter
    }

    fun setContactsAccount(accounts: List<ContactsAccount>) {
        adapter.clear()
        adapter.addAll(accounts)
    }
}
