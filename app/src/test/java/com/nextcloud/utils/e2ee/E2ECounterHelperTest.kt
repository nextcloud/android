/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.e2ee

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class E2ECounterHelperTest {
    @Test
    fun usesTaskAccountCapabilityWithoutReadingForegroundAccount() {
        val folder: OCFile = mock { on { e2eCounter } doReturn 5L }
        val accountA: OCCapability = mock { on { endToEndEncryptionApiVersion } doReturn E2EVersion.V1_2 }
        val accountB: OCCapability = mock { on { endToEndEncryptionApiVersion } doReturn E2EVersion.V2_0 }

        assertEquals(-1L, E2ECounterHelper.getCounter(accountA, folder, null))
        assertEquals(6L, E2ECounterHelper.getCounter(accountB, folder, null))
        assertEquals(-1L, E2ECounterHelper.getCounter(accountA, folder, null))
    }
}
