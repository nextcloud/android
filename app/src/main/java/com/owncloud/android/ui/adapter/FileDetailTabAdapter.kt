/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextcloud.client.account.User
import com.nextcloud.ui.fileInfo.FileInfoFragment
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment

class FileDetailTabAdapter(
    private val fragmentActivity: FragmentActivity,
    private val file: OCFile,
    private val user: User,
    private val showSharingTab: Boolean,
    private val showDetailsTab: Boolean
) : FragmentStateAdapter(fragmentActivity) {

    private enum class Tab(val position: Int) {
        Activities(0),
        Sharing(1),
        Details(2)
    }

    var fileDetailSharingFragment: FileDetailSharingFragment? = null
        private set
    var fileDetailActivitiesFragment: FileDetailActivitiesFragment? = null
        private set

    override fun createFragment(position: Int): Fragment = when (position) {
        Tab.Sharing.position -> FileDetailSharingFragment.newInstance(file, user)
            .also { fileDetailSharingFragment = it }

        Tab.Details.position -> FileInfoFragment.newInstance(file, user)

        else -> FileDetailActivitiesFragment.newInstance(file, user)
            .also { fileDetailActivitiesFragment = it }
    }

    override fun getItemCount(): Int {
        // always show Activities
        var count = 1

        if (showSharingTab) {
            count++
        }

        if (showDetailsTab) {
            count++
        }

        return count
    }
}
