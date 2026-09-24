/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.helpers

import android.content.ContentResolver
import android.net.Uri
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UriUploaderTest {

    private companion object {
        const val PACKAGE_NAME = "com.nextcloud.client"
        const val OWN_FILE_PROVIDER_AUTHORITY = "org.nextcloud.files"
    }

    private lateinit var activity: FileActivity
    private lateinit var sut: UriUploader

    @Before
    fun setUp() {
        activity = mock()
        whenever(activity.packageName).thenReturn(PACKAGE_NAME)
        whenever(activity.getString(R.string.authority)).thenReturn("org.nextcloud")
        whenever(activity.getString(R.string.file_provider_authority)).thenReturn(OWN_FILE_PROVIDER_AUTHORITY)
        whenever(activity.getString(R.string.document_provider_authority)).thenReturn("org.nextcloud.documents")
        whenever(activity.getString(R.string.image_cache_provider_authority))
            .thenReturn("org.nextcloud.imageCache.provider")
        whenever(activity.getString(R.string.users_and_groups_search_authority))
            .thenReturn("com.nextcloud.android.providers.UsersAndGroupsSearchProvider")

        sut = UriUploader(
            activity,
            emptyList(),
            "",
            mock<User>(),
            0,
            showWaitingDialog = false,
            copyTmpTaskListener = null
        )
    }

    @Test
    fun testIsSensitiveUriWhenGivenStandardExternalUriShouldReturnFalse() {
        val uri = contentUri(authority = "com.example.otherapp.fileprovider")

        assertFalse(sut.isSensitiveUri(uri))
    }

    @Test
    fun testIsSensitiveUriWhenGivenEmbeddedFairScanUriShouldReturnFalse() {
        // FairScan's own FileProvider authority is built from the host app's applicationId once embedded,
        // so it looks like it belongs to this app even though it doesn't point at this app's own data.
        val uri = contentUri(authority = "$PACKAGE_NAME.fileprovider")

        assertFalse(sut.isSensitiveUri(uri))
    }

    @Test
    fun testIsSensitiveUriWhenGivenOwnFileProviderUriShouldReturnTrue() {
        val uri = contentUri(authority = OWN_FILE_PROVIDER_AUTHORITY)

        assertTrue(sut.isSensitiveUri(uri))
    }

    // android.net.Uri.parse() doesn't work in these plain JVM unit tests, so build a mock instead.
    private fun contentUri(authority: String): Uri {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        whenever(uri.authority).thenReturn(authority)
        return uri
    }
}
