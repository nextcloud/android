/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FileDetailActivitiesFragmentIT : AbstractIT() {

    @Test
    fun newInstanceStoresFileAndUserInArguments() {
        val file = OCFile("/test.txt").apply { fileId = 12 }

        val fragment = FileDetailActivitiesFragment.newInstance(file, user)

        val arguments = fragment.arguments
        assertNotNull(arguments)

        val storedFile = arguments.getParcelableArgument("FILE", OCFile::class.java)
        val storedUser = arguments.getParcelableArgument("USER", User::class.java)

        assertEquals(file.fileId, storedFile?.fileId)
        assertEquals(user.accountName, storedUser?.accountName)
    }
}
