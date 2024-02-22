/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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
