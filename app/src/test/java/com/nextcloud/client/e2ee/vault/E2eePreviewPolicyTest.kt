/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class E2eePreviewPolicyTest {
    @Test
    fun serverPreviewIsBlockedForEncryptedFiles() {
        val file = OCFile("/Photos/secret.jpg").apply {
            isEncrypted = true
        }

        assertFalse(E2eePreviewPolicy.isServerPreviewAllowed(file))
    }

    @Test
    fun serverPreviewIsAllowedForNormalFiles() {
        val file = OCFile("/Photos/normal.jpg").apply {
            isEncrypted = false
        }

        assertTrue(E2eePreviewPolicy.isServerPreviewAllowed(file))
    }
}
