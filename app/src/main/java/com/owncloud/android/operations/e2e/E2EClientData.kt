/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import com.owncloud.android.lib.common.OwnCloudClient

data class E2EClientData(val client: OwnCloudClient, val token: String, val publicKey: String)
