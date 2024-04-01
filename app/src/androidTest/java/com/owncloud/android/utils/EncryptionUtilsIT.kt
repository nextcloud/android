/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptionUtilsIT : AbstractIT() {
    @Throws(
        java.security.NoSuchAlgorithmException::class,
        java.io.IOException::class,
        org.bouncycastle.operator.OperatorCreationException::class
    )
    @Test
    fun saveAndRestorePublicKey() {
        val arbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val keyPair = EncryptionUtils.generateKeyPair()
        val e2eUser = "e2e-user"
        val key = CsrHelper().generateCsrPemEncodedString(keyPair, e2eUser)

        EncryptionUtils.savePublicKey(user, key, e2eUser, arbitraryDataProvider)

        assertEquals(key, EncryptionUtils.getPublicKey(user, e2eUser, arbitraryDataProvider))
    }
}
