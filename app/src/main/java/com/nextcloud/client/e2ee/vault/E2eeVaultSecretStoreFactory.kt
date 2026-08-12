/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.owncloud.android.datamodel.ArbitraryDataProvider

object E2eeVaultSecretStoreFactory {
    @JvmStatic
    fun create(arbitraryDataProvider: ArbitraryDataProvider): E2eeVaultSecretStore = E2eeVaultSecretStore(
        arbitraryDataProvider,
        AndroidKeystoreE2eeVaultSecretCipher(E2eeVaultSessionConfig())
    )
}
