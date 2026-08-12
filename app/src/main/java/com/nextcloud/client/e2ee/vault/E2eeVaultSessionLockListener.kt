/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

interface E2eeVaultSessionLockListener {
    fun onVaultLocked(key: E2eeVaultSessionKey)

    fun onAllVaultsLocked()
}
