/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.fragment

import android.Manifest
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.rule.GrantPermissionRule
import com.nextcloud.client.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.shares.OCShare
import org.junit.Rule
import org.junit.Test

class FileDetailSharingFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun listShares_file_none() {
        val sut = testActivityRule.launchActivity(null)
        sut.addFragment(FileDetailSharingFragment())

    }

    @Test
    fun listShares_file_all() {
        // with multiple public share links
        throw NotImplementedError()
    }

    @Test
    fun listShares_folder_none() {
        throw NotImplementedError()
    }

    @Test
    fun listShares_folder_all() {
        // with multiple public share links
        throw NotImplementedError()
    }

    @Test
    fun publicLink_optionMenu() {
        val sut = FileDetailSharingFragment()

        val overflowMenuShareLink = ImageView(targetContext)
        val popup = PopupMenu(targetContext, overflowMenuShareLink)
        popup.inflate(R.menu.fragment_file_detail_sharing_public_link)
        val publicShare = OCShare()

        sut.prepareLinkOptionsMenu(popup.menu, publicShare)

        // TODO check all options

        // scenarios: public link, email, …, both for file/folder
    }
}
