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
    fragmentActivity: FragmentActivity,
    private val file: OCFile,
    private val user: User,
    private val showSharingTab: Boolean
) : FragmentStateAdapter(fragmentActivity) {
    var fileDetailSharingFragment: FileDetailSharingFragment? = null
        private set
    var fileDetailActivitiesFragment: FileDetailActivitiesFragment? = null
        private set

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> {
                fileDetailSharingFragment = FileDetailSharingFragment.newInstance(file, user)
                fileDetailSharingFragment
            }

            2 -> {
                FileInfoFragment.newInstance(file, user)
            }

            else -> {
                fileDetailActivitiesFragment = FileDetailActivitiesFragment.newInstance(file, user)
                fileDetailActivitiesFragment
            }
        }!!
    }

    override fun getItemCount(): Int {
        return if (showSharingTab) {
            3
        } else {
            2
        }
    }
}
